package com.flights.studio

import android.webkit.WebView

object FlightsTabsInjector {

    private fun loadCss(view: WebView?): String = try {
        view?.context?.assets?.open("fs_flights_style.css")?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (_: Exception) { "" }

    fun injectHideTriggers(view: WebView?, showFlightsTabs: Boolean, isFlightsMain: Boolean) {
        val css = loadCss(view)
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        val js = $$"""
        (function() {
        'use strict';

        var SHOW_TABS    = $${if (showFlightsTabs) "true" else "false"};
        var IS_MAIN      = $${if (isFlightsMain)  "true" else "false"};

        // ─────────────────────────────────────────────────────────────
        // CONSTANTS
        // ─────────────────────────────────────────────────────────────
        const JAC_LAT    = 43.6073;
        const JAC_LON    = -110.7377;
        const DOCK_PEEK  = 165;          // px visible when docked
        const AC_TTL     = 12000;        // aircraft cache TTL ms
        const WX_TTL     = 5000;         // weather cache TTL ms
        const LIVE_INTERVAL = 15000;     // live arrivals refresh ms

        const AIRPORT_CODES = {
          "Salt Lake City":"SLC","Denver":"DEN","Newark":"EWR","Chicago":"ORD",
          "Dallas/Fort Worth":"DFW","Los Angeles":"LAX","San Francisco":"SFO",
          "Atlanta":"ATL","Seattle":"SEA","Phoenix":"PHX","Minneapolis":"MSP",
          "Detroit":"DTW","Houston":"IAH","Boston":"BOS","Washington":"DCA",
          "Charlotte":"CLT","Miami":"MIA","New York":"JFK","Las Vegas":"LAS","Portland":"PDX"
        };
        const AIRLINE_PREFIXES = {
          DAL:'Delta', DL:'Delta',
          UAL:'United', UA:'United',
          AAL:'American', AA:'American',
          ASA:'Alaska', AS:'Alaska',
          SKW:'SkyWest',
          SWA:'Southwest', WN:'Southwest',
          JBU:'JetBlue', B6:'JetBlue',
          FFT:'Frontier', F9:'Frontier',
          NKS:'Spirit', NK:'Spirit',
          EDV:'Endeavor Air',
          RPA:'Republic Airways',
          ENY:'Envoy Air',
          QXE:'Horizon Air',
          PDT:'Piedmont',
          ASH:'Mesa Airlines',
          GJS:'GoJet Airlines',
          UCA:'CommutAir',
          TSS:'TriState Aviation'
        };

        const BBOX = { lamin:30, lomin:-125, lamax:50, lomax:-88 };

        // ─────────────────────────────────────────────────────────────
        // STATE
        // ─────────────────────────────────────────────────────────────
        let wxCache = null,  wxAt  = 0;
        let acCache = null,  acAt  = 0,  acIndex = {}, acFetching = false, acOk = false;
        let scrapeCache = null, scrapeDirty = true;

        // ─────────────────────────────────────────────────────────────
        // SETTINGS HELPERS
        // ─────────────────────────────────────────────────────────────
        function getSettings() {
          const defaults = {
            liveArrivals: true,
            weather: true,
            inbound: true,
            confidence: true,
            colors: true,
            distance: 80
          };
          try {
            return Object.assign(defaults, JSON.parse(localStorage.getItem('fs-settings') || '{}'));
          } catch {
            return defaults;
          }
        }
        function saveSettings(s) {
          localStorage.setItem('fs-settings', JSON.stringify(s));
        }

        // ─────────────────────────────────────────────────────────────
        // IDLE SCHEDULER (rIC with rAF fallback)
        // ─────────────────────────────────────────────────────────────
        const fsIdle = typeof requestIdleCallback === 'function'
          ? (fn, ms) => requestIdleCallback(fn, { timeout: ms || 600 })
          : (fn)     => setTimeout(fn, 0);

        // ─────────────────────────────────────────────────────────────
        // METRICS — sheet dimensions relative to tab bar
        // ─────────────────────────────────────────────────────────────
        function metrics() {
          const tab  = document.getElementById('fs-bottom-tabs');
          const docked = getSettings().dock === true;
          const avail= docked ? window.innerHeight : (tab ? Math.max(260, tab.getBoundingClientRect().top) : window.innerHeight);
          const h    = Math.round(avail * 0.91);
          return { h, dock: Math.max(0, h - DOCK_PEEK), hide: h + 40 };
        }

        // ─────────────────────────────────────────────────────────────
        // UTILITIES
        // ─────────────────────────────────────────────────────────────
        function haversine(la1, lo1, la2, lo2) {
          const R = 3958.8, d2r = Math.PI / 180;
          const dLa = (la2 - la1) * d2r, dLo = (lo2 - lo1) * d2r;
          const a = Math.sin(dLa/2)**2 + Math.cos(la1*d2r)*Math.cos(la2*d2r)*Math.sin(dLo/2)**2;
          return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        }
        function bearingTo(la1, lo1, la2, lo2) {
          const d2r = Math.PI / 180, r2d = 180 / Math.PI;
          const y = Math.sin((lo2 - lo1) * d2r) * Math.cos(la2 * d2r);
          const x = Math.cos(la1 * d2r) * Math.sin(la2 * d2r)
            - Math.sin(la1 * d2r) * Math.cos(la2 * d2r) * Math.cos((lo2 - lo1) * d2r);
          return (Math.atan2(y, x) * r2d + 360) % 360;
        }
        function angleDiff(a, b) {
          if (a == null || b == null) return 180;
          return Math.abs(((a - b + 540) % 360) - 180);
        }

        function clockToDate(str) {
          if (!str) return null;
          const s  = String(str).toLowerCase().trim();
          const pm = s.includes('pm'), am = s.includes('am');
          const [hh, mm] = s.replace('am','').replace('pm','').trim().split(':').map(Number);
          if (isNaN(hh) || isNaN(mm)) return null;
          let h = hh;
          if (pm && h !== 12) h += 12;
          if (am && h === 12) h  = 0;
          const d = new Date(); d.setHours(h, mm, 0, 0); return d;
        }
        function clockToMins(str) { const d = clockToDate(str); return d ? d.getHours()*60+d.getMinutes() : null; }
        function minsAgo(str) {
          const m = clockToMins(str); if (m == null) return null;
          const now = new Date().getHours()*60 + new Date().getMinutes();
          let d = now - m;
          if (d < -720) d += 1440; if (d > 720) d -= 1440;
          return d;
        }
        function recentLanding(f) { const m = minsAgo(f.actual || f.sched || ''); return m != null && m >= 0 && m <= 90; }
        function escHtml(value) {
          return String(value ?? '').replace(/[&<>"']/g, ch => ({
            '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
          })[ch]);
        }
        function cleanDayText(el) {
          const raw = el?.dataset?.fsDayLabel
            || el?.querySelector?.('.fs-day-label')?.textContent
            || el?.textContent
            || '';
          return raw.replace(/\s+/g, ' ').trim()
            .replace(/\s+\d+\s+flights?\s+total.*$/i, '')
            .replace(/\s+last\s+updated.*$/i, '')
            .replace(/\s+last\s+update\s*:.*$/i, '')
            .trim();
        }
        function signedTimeDiffMins(sched, actual) {
          const s = clockToMins(sched), a = clockToMins(actual);
          if (s == null || a == null) return null;
          let d = a - s;
          if (d < -720) d += 1440;
          if (d > 720) d -= 1440;
          return d;
        }
        function fmtSignedMins(mins) {
          const abs = Math.abs(Math.round(mins || 0));
          if (abs === 0) return 'on schedule';
          const label = fmtMin(abs);
          return mins > 0 ? `${label} late` : `${label} early`;
        }

        function applyFlightRowStatusClasses() {
          const con = document.getElementById('flight-container');
          if (!con) return;
          con.querySelectorAll('table.jha-flights tbody tr').forEach(row => {
            if (row.querySelector('.day') || row.querySelector('th')) return;
            row.classList.remove('fs-row-arrived','fs-row-diverted','fs-row-cancelled');
            const statusEl = row.querySelector('.status span') || row.querySelector('.status');
            const status = (statusEl?.textContent || '').trim().toLowerCase();
            if (!status) return;
            if (status.includes('cancel')) {
              row.classList.add('fs-row-cancelled');
            } else if (status.includes('divert')) {
              row.classList.add('fs-row-diverted');
            } else if (status.includes('arrived')) {
              row.classList.add('fs-row-arrived');
            }
          });
        }

        function applyFlightDateCounts() {
          const con = document.getElementById('flight-container');
          if (!con) return;
          const updatedEl = con.querySelector('.flight-table__time');
          let updatedText = (updatedEl?.textContent || '').replace(/\s+/g, ' ').trim();
          updatedText = updatedText
            .replace(/^last\s*update\s*:?\s*/i, '')
            .replace(/^last\s*updated\s*:?\s*/i, '')
            .replace(/^updated\s*:?\s*/i, '')
            .replace(/\s*([ap])\.?m\.?\b/ig, (_, period) => period.toLowerCase() + 'm')
            .trim();
          const updatedLabel = updatedText ? `last updated ${updatedText}` : '';
          con.querySelectorAll('table.jha-flights tbody').forEach(body => {
            const rows = [...body.querySelectorAll('tr')];
            rows.forEach((row, index) => {
              const day = row.querySelector('.day');
              if (!day) return;
              if (!day.dataset.fsDayLabel) {
                day.dataset.fsDayLabel = day.textContent.trim();
              }
              let count = 0;
              for (let i = index + 1; i < rows.length; i++) {
                if (rows[i].querySelector('.day')) break;
                if (rows[i].querySelector('td.airline,td.flight,.airline,.flight')) count++;
              }
              const label = day.dataset.fsDayLabel || '';
              const suffix = `${count} flight${count === 1 ? '' : 's'} total`;
              const updated = updatedLabel ? `<span class="fs-day-updated">${updatedLabel}</span>` : '';
              const nextHtml = `<span class="fs-day-label">${label}</span><span class="fs-day-count">${suffix}</span>${updated}`;
              if (day.innerHTML !== nextHtml) {
                day.innerHTML = nextHtml;
              }
            });
          });
        }

        function applyEnhancedTableCells() {
          const con = document.getElementById('flight-container');
          if (!con) return;
          con.querySelectorAll('table.jha-flights tbody tr').forEach(row => {
            if (row.querySelector('.day') || row.querySelector('th')) return;
            ['airline','flight','from','sched','actual'].forEach(cls => {
              const cell = row.querySelector(`td.${cls}`);
              if (!cell || Array.from(cell.children).some(child => child.classList?.contains('fs-cell-chip'))) return;
              const chip = document.createElement('span');
              chip.className = `fs-cell-chip fs-cell-${cls}`;
              while (cell.firstChild) chip.appendChild(cell.firstChild);
              cell.appendChild(chip);
            });
          });
        }

        function fmtMin(total) {
          if (total == null) return '';
          const m = Math.max(0, Math.round(total)), h = Math.floor(m/60), mn = m%60;
          return h>0 && mn>0 ? `${h}h ${mn}m` : h>0 ? `${h}h` : `${mn}m`;
        }
        function fmtDelay(minutes) {
          if (!minutes || minutes <= 0) return '';
          const h = Math.floor(minutes/60), m = minutes%60;
          return h>0 && m>0 ? `${h}h${m}min` : h>0 ? `${h}h` : `${m}min`;
        }
        function delayMins(sched, actual) {
          const s = clockToMins(sched), a = clockToMins(actual);
          if (s == null || a == null) return 0;
          let d = a - s; if (d < 0) { if (d < -720) d += 1440; else return 0; } return d;
        }
        function etaMins(dist, speed) { return speed >= 60 ? Math.round((dist/speed)*60) : null; }
        function airportCode(label) { return AIRPORT_CODES[label] || label || ''; }

        function tempColor(f) {
          const t  = parseFloat(f); if (isNaN(t)) return '';
          const dk = window.matchMedia('(prefers-color-scheme: dark)').matches;
          const sc = dk
            ? [{t:10,c:'#3d8cff'},{t:25,c:'#2fb7ff'},{t:40,c:'#39d4ff'},{t:55,c:'#3ee0b3'},
               {t:70,c:'#6df26d'},{t:80,c:'#ffe066'},{t:90,c:'#ff9f40'},{t:100,c:'#ff6b4a'},{t:999,c:'#ff3b3b'}]
            : [{t:10,c:'#0d47a1'},{t:25,c:'#1976d2'},{t:40,c:'#0288d1'},{t:55,c:'#26a69a'},
               {t:70,c:'#7cb342'},{t:80,c:'#fbc02d'},{t:90,c:'#f57c00'},{t:100,c:'#c62828'},{t:999,c:'#8b0000'}];
          return (sc.find(s => t <= s.t) || sc[sc.length-1]).c;
        }
        function wxClass(wx) {
          const t = parseInt(wx?.tempF); if (isNaN(t)) return '';
          return t<=32?'wx-freezing':t<=45?'wx-cool':t<=70?'wx-mild':t<=90?'wx-warm':'wx-hot';
        }
        
         // ─────────────────────────────────────────────────────────────
        // Formate Updated Label
        // ─────────────────────────────────────────────────────────────
        function formatUpdatedLabel(flashSeconds) {
          const now = new Date();

          const hours = now.toLocaleTimeString([], {
            hour: '2-digit',
            hour12: true
          }).match(/^\d{1,2}/)?.[0] || '';

          const mins = String(now.getMinutes()).padStart(2, '0');
          const secs = String(now.getSeconds()).padStart(2, '0');
          const ampm = now.getHours() >= 12 ? 'PM' : 'AM';

          return `<span class="fs-live-updated-text">Updated ${hours}:${mins}</span><span class="fs-live-seconds${flashSeconds ? ' fs-live-seconds-flash' : ''}">:${secs}</span><span class="fs-live-updated-text"> ${ampm}</span>`;
        }

        // ─────────────────────────────────────────────────────────────
        // WEATHER SCRAPE
        // ─────────────────────────────────────────────────────────────
        function scrapeWeather() {
          const qs = s => { try { return document.querySelector(s); } catch { return null; } };
          const fF = qs('.cur-fahren,.fahrenheit,.temp-f');
          const fC = qs('.cur-celcius,.celsius,.temp-c');
          const iW = qs('.icon-wrap,.cur-icon,.weather-icon,.wx-icon');
          const tempF = (fF?.textContent.trim() || '').replace('/','').trim();
          const tempC = fC?.textContent.trim() || '';
          let icon = '';
          if (iW) { const svg = iW.querySelector('svg'); if (svg) icon = svg.outerHTML; }
          let windSpeed=null, windDir='', windUnit=null, vis=null, cloud=null;
          const det = document.querySelector('.cur-details');
          if (det) {
            const tx = det.innerText.replace(/\u00A0/g,' ').replace(/\s+/g,' ');
            const vm = tx.match(/visibility[^0-9]*([\d.]+)/i);   if (vm) vis = parseFloat(vm[1]);
            const wm = tx.match(/wind[^0-9]*([\d]+)\s*(mph|kt)?\s*([A-Z]+)?/i);
            if (wm) { windSpeed=parseInt(wm[1],10); windUnit=wm[2]||'mph'; windDir=wm[3]||''; }
            const cm = tx.match(/cloud[^0-9]*([\d]+)/i);         if (cm) cloud = parseInt(cm[1]);
          }
          return { tempF, tempC, temp: (tempF&&tempC)?`${tempF} / ${tempC}`:tempF||tempC, windSpeed, windDir, windUnit, vis, cloud, icon };
        }
        function getWx() {
          if (!wxCache || Date.now()-wxAt > WX_TTL) { wxCache = scrapeWeather(); wxAt = Date.now(); }
          return wxCache;
        }

        function weatherCondition(wx) {
          const raw = `${wx?.icon || ''} ${wx?.temp || ''}`.toLowerCase();
          const cloud = parseInt(wx?.cloud);
          const vis = parseFloat(wx?.vis);
          if (/thunder|lightning|storm|t-?storm/.test(raw)) return 'thunder';
          if (/rain|shower|storm|drizzle|precip/.test(raw)) return 'rain';
          if (!isNaN(vis) && vis <= 2) return 'cloudy';
          if (!isNaN(cloud) && cloud >= 70) return 'cloudy';
          if (!isNaN(cloud) && cloud >= 30) return 'partly';
          const hour = new Date().getHours();
          return hour >= 6 && hour < 20 ? 'sunny' : 'night';
        }

        function syncWeatherSnapshot() {
          try {
            if (!window.FlightsAndroidBridge?.updateWeatherSnapshot) return;
            const wx = getWx();
            if (!wx?.temp) return;
            const condition = weatherCondition(wx);
            const detail = [];
            if (wx.windSpeed) detail.push(`Wind ${wx.windSpeed} ${wx.windUnit || 'mph'} ${wx.windDir || ''}`.trim());
            if (wx.cloud != null) detail.push(`Cloud ${wx.cloud}%`);
            if (wx.vis != null) detail.push(`Vis ${wx.vis} mi`);
            window.FlightsAndroidBridge.updateWeatherSnapshot(JSON.stringify({
              temp: wx.temp,
              condition,
              summary: detail.join(' • '),
              source: 'airport_web',
              updatedAt: Date.now()
            }));
          } catch (e) {}
        }

        function wxWarnings(wx) {
          const out = [];
          if (wx.vis != null && wx.vis <= 3)
            out.push(`<span class="fs-wx-warn vis-warning"><svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor"><path d="M792-56 624-222q-35 11-70.5 16.5T480-200q-151 0-269-83.5T40-500q21-53 53-98.5t73-81.5L56-792l56-56 736 736-56 56ZM480-320q11 0 20.5-1t20.5-4L305-541q-3 11-4 20.5t-1 20.5q0 75 52.5 127.5T480-320Zm292 18L645-428q7-17 11-34.5t4-37.5q0-75-52.5-127.5T480-680q-20 0-37.5 4T408-664L306-766q41-17 84-25.5t90-8.5q151 0 269 83.5T920-500q-23 59-60.5 109.5T772-302ZM587-486 467-606q28-5 51.5 4.5T559-574q17 18 24.5 41.5T587-486Z"/></svg>Low Vis</span>`);
          if (wx.windSpeed >= 20)
            out.push(`<span class="fs-wx-warn wind-warning"><svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor"><path d="M460-160q-50 0-85-35t-35-85h80q0 17 11.5 28.5T460-240q17 0 28.5-11.5T500-280q0-17-11.5-28.5T460-320H80v-80h380q50 0 85 35t35 85q0 50-35 85t-85 35ZM80-560v-80h540q26 0 43-17t17-43q0-26-17-43t-43-17q-26 0-43 17t-17 43h-80q0-59 40.5-99.5T620-840q59 0 99.5 40.5T760-700q0 59-40.5 99.5T620-560H80Zm660 320v-80q26 0 43-17t17-43q0-26-17-43t-43-17H80v-80h660q59 0 99.5 40.5T880-380q0 59-40.5 99.5T740-240Z"/></svg>Crosswind</span>`);
          if (parseInt(wx.tempF) <= 36)
            out.push(`<span class="fs-wx-warn ice-warning"><svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor"><path d="M800-560q-17 0-28.5-11.5T760-600q0-17 11.5-28.5T800-640q17 0 28.5 11.5T840-600q0 17-11.5 28.5T800-560ZM400-80v-144L296-120l-56-56 160-160v-64h-64L176-240l-56-56 104-104H80v-80h144L120-584l56-56 160 160h64v-64L240-704l56-56 104 104v-144h80v144l104-104 56 56-160 160v64h320v80H656l104 104-56 56-160-160h-64v64l160 160-56 56-104-104v144h-80Zm360-600v-200h80v200h-80Z"/></svg>Ice Risk</span>`);
          return out.join(' • ');
        }

        // ─────────────────────────────────────────────────────────────
        // AIRCRAFT SOURCES
        // ─────────────────────────────────────────────────────────────
        async function fetchJson(proxyPath, directUrl, sourceName) {
          try {
            const r = await fetch(proxyPath, { cache: 'no-store' });
            if (r?.ok) return await r.json();
          } catch {}
          const r = await fetch(directUrl, { cache: 'no-store' });
          if (!r?.ok) throw new Error(sourceName + ' ' + r?.status);
          return await r.json();
        }
        async function fetchAdsbLol() {
          const d = await fetchJson(
            '/__fs_proxy/adsb_lol',
            `https://api.adsb.lol/v2/lat/${JAC_LAT}/lon/${JAC_LON}/dist/500`,
            'adsb.lol'
          );
          if (!d?.ac) throw new Error('adsb.lol no ac');
          return d.ac.map(a => {
            const alt = (typeof a.alt_baro==='number' ? a.alt_baro : (a.alt_geom ?? 0));
            return [a.hex, (a.flight||a.r||'').trim(), null,null,null, a.lon??null, a.lat??null,
                    alt*0.3048, null, (a.gs??0)/1.94384, a.track??null, (a.baro_rate??a.geom_rate??0)*0.00508, null, alt*0.3048];
          });
        }
        async function fetchAdsbFi() {
          const d = await fetchJson(
            '/__fs_proxy/adsb_fi',
            `https://opendata.adsb.fi/api/v3/lat/${JAC_LAT}/lon/${JAC_LON}/dist/250`,
            'adsb.fi'
          );
          const aircraft = d?.ac || d?.aircraft;
          if (!aircraft?.length) throw new Error('adsb.fi no aircraft');
          return aircraft.map(a => {
            const alt = (typeof a.alt_baro==='number' ? a.alt_baro : (a.alt_geom ?? a.altitude ?? 0));
            return [a.hex||a.icao, (a.flight||a.callsign||a.registration||a.r||'').trim(), null,null,null,
                    a.lon??null, a.lat??null, alt*0.3048, null, (a.gs??a.speed??0)/1.94384,
                    a.track??a.heading??null, (a.baro_rate??a.geom_rate??a.vert_rate??0)*0.00508, null, alt*0.3048];
          });
        }

        async function refreshAcCache() {
          if (acFetching || (acCache && Date.now()-acAt < AC_TTL)) return;
          acFetching = true;
          try {
            let states = null;
            try { states = await fetchAdsbLol(); } catch {}
            if (!states) try { states = await fetchAdsbFi(); } catch {}
            try {
              if (!states) {
                const d = await fetchJson(
                  '/__fs_proxy/opensky',
                  `https://opensky-network.org/api/states/all?lamin=${BBOX.lamin}&lomin=${BBOX.lomin}&lamax=${BBOX.lamax}&lomax=${BBOX.lomax}`,
                  'opensky'
                );
                if (d?.states?.length) states = d.states;
              }
            } catch {}
            if (!states?.length) {
              acOk = false;
              return;
            }
            acOk    = true;
            acCache = states;
            window._fsAcStates = states;
            acAt    = Date.now();
            acIndex = {};
            for (const s of acCache) {
              if (!s?.[1]) continue;
              const k = s[1].trim().replace(/\s+/g,'').toUpperCase();
              if (!k) continue;
              acIndex[k] = s;
              const d = k.replace(/[A-Z]/g,'');
              if (d) acIndex[d] = s;
            }
            renderInbound(detectInbound(acCache));
            } catch {
              acOk = false;
            } finally { acFetching = false; }
        }

        // ─────────────────────────────────────────────────────────────
        // AIRCRAFT LOOKUP  (O(1) index + fuzzy scored fallback)
        // single canonical implementation used everywhere
        // ─────────────────────────────────────────────────────────────
        function lookupCallsign(callsign) {
          if (!acCache || !callsign) return null;
          const norm = callsign.trim().replace(/\s+/g,'').toUpperCase();
          const dig  = norm.replace(/[A-Z]/g,'');
          let s = acIndex[norm] || (dig ? acIndex[dig] : null);
          if (!s) {
            for (const x of acCache) {
              if (!x?.[1]) continue;
              const k = x[1].trim().replace(/\s+/g,'').toUpperCase();
              if (k === norm || (dig && (k.endsWith(dig) || k.replace(/[A-Z]/g,'') === dig))) { s = x; break; }
            }
          }
          return s ? acFromState(s) : null;
        }

        function acFromState(s) {
          const lat = s[6], lon = s[5];
          if (lat == null || lon == null) return null;
          return {
            callsign: (s[1]||'').trim(), rawCallsign: (s[1]||'').trim(),
            altitude: Math.round((s[13]||s[7]||0)*3.28084),
            speed:    Math.round((s[9]||0)*1.94384),
            heading:  s[10] != null ? Math.round(s[10]) : null,
            vRate:    s[11]||0, lat, lon
          };
        }

        // Fuzzy search through full cache — shared by both live-arrivals build and updater
        function fuzzyMatch(candidates) {
          if (!acCache || !candidates?.length) return null;
          const digits = [...new Set(
            candidates.map(c => (c||'').toUpperCase().replace(/[A-Z\s]/g,'')).filter(Boolean)
          )];
          let best = null, bestScore = -Infinity;
          for (const s of acCache) {
            if (!s?.[1]) continue;
            const k   = s[1].trim().replace(/\s+/g,'').toUpperCase(); if (!k) continue;
            const lat = s[6], lon = s[5]; if (lat==null || lon==null) continue;
            const alt = Math.round((s[13]||s[7]||0)*3.28084);
            const spd = Math.round((s[9]||0)*1.94384);
            const vr  = s[11]||0;
            const dist= haversine(lat,lon,JAC_LAT,JAC_LON);
            const dg  = k.replace(/[A-Z]/g,'');
            if (!dg || dist > 1500 || (spd < 40 && alt < 500)) continue;
            let score = 0;
            for (const tgt of digits) {
              if (!tgt) continue;
              if (dg === tgt)           { score += 200; break; }
              if (dg.endsWith(tgt))     { score += 150; break; }
              if (dg.includes(tgt))     { score +=  80; break; }
            }
            if (score === 0) continue;
            score += dist<=50?100 : dist<=150?70 : dist<=300?40 : dist<=500?15 : dist<=800?5 : 0;
            score += alt<1000&&dist>20 ? -40 : alt>=1000&&alt<=10000 ? 30 : alt>10000&&alt<=45000 ? 20 : 0;
            score += vr<-2&&dist<200   ?  25 : vr>5 ? -10 : 0;
            score += spd>=120&&spd<=320?  15 : spd<60 ? -20 : 0;
            if (score > bestScore) { bestScore = score; best = s; }
          }
          return bestScore >= 200 ? acFromState(best) : null;
        }

        async function matchCandidates(candidates) {
          await refreshAcCache();
          if (!acCache) return null;
          for (const c of candidates) { const r = lookupCallsign(c); if (r) return r; }
          return fuzzyMatch(candidates);
        }

        // ─────────────────────────────────────────────────────────────
        // CALLSIGN CANDIDATES  (airline name → ICAO prefix list)
        // ─────────────────────────────────────────────────────────────
        function candidates(airline, flight) {
          const raw  = (flight||'').toUpperCase().trim().replace(/[^A-Z0-9]/g,'');
          const digs = raw.replace(/^[A-Z]{2,3}/,'').replace(/[A-Z]/g,'');
          const a    = (airline||'').toLowerCase();
          const pfx  =
            a.includes('united')      ? ['UAL','UA','SKW','GJS','UCA','RPA','ASH','TSS'] :
            a.includes('delta')       ? ['DAL','DL','SKW','EDV','RPA'] :
            a.includes('american')    ? ['AAL','AA','ENY','PDT','PSA','SKW','RPA'] :
            a.includes('alaska')      ? ['ASA','AS','QXE','SKW','OEN'] :
            a.includes('skywest')     ? ['SKW'] :
            a.includes('southwest')   ? ['SWA','WN'] :
            a.includes('jetblue')     ? ['JBU','B6'] :
            a.includes('frontier')    ? ['FFT','F9'] :
            a.includes('spirit')      ? ['NKS','NK'] :
            a.includes('horizon')     ? ['QXE','AS'] :
            a.includes('republic')    ? ['RPA'] :
            a.includes('endeavor')    ? ['EDV'] :
            a.includes('envoy')       ? ['ENY'] :
            a.includes('piedmont')    ? ['PDT'] :
            a.includes('mesa')        ? ['ASH'] :
            a.includes('gojet')       ? ['GJS'] : [];
          const out = [];
          pfx.forEach(p => { if (digs) { out.push(p+digs); out.push(`${p} ${digs}`); } });
          if (raw)  out.push(raw);
          if (digs) out.push(digs);
          return [...new Set(out)];
        }

        function callsignParts(cs) {
          const c = (cs||'').trim().toUpperCase().replace(/\s+/g,'');
          const m = c.match(/^([A-Z]{2,3})(\d+[A-Z]?)$/);
          return { norm: c, prefix: m ? m[1] : c.replace(/[^A-Z]/g,'').slice(0,3), flight: m ? m[2] : c.replace(/[A-Z]/g,'') };
        }
        function airlineFromCallsign(cs) {
          const p = callsignParts(cs).prefix;
          return AIRLINE_PREFIXES[p] || AIRLINE_PREFIXES[p.slice(0,2)] || '';
        }
        function flightNumberFromCallsign(cs) {
          return callsignParts(cs).flight || '';
        }
        function operatorFromCallsign(cs) {
          const c = (cs||'').trim().toUpperCase().replace(/\s+/g,'');
          if (c.startsWith('SKW')) return 'SkyWest';
          if (c.startsWith('RPA')) return 'Republic Airways';
          if (c.startsWith('EDV')) return 'Endeavor Air';
          if (c.startsWith('ENY')) return 'Envoy Air';
          if (c.startsWith('PDT')) return 'Piedmont';
          if (c.startsWith('QXE')) return 'Horizon Air';
          if (c.startsWith('ASH')) return 'Mesa Airlines';
          if (c.startsWith('GJS')) return 'GoJet Airlines';
          if (c.startsWith('PSA')) return 'PSA Airlines';
          if (c.startsWith('UCA')) return 'CommutAir';
          if (c.startsWith('TSS')) return 'TriState Aviation';
          return '';
        }
        function operatingCarrier(airline, flight) {
          const a = (airline||'').toLowerCase(), f = (flight||'').toUpperCase().trim();
          if (a.includes('delta')    && /^[4-9]\d{3}$/.test(f)) return 'SkyWest';
          if (a.includes('united')   && /^[4-9]\d{3}$/.test(f)) return 'SkyWest';
          if (a.includes('american') && /^[3-5]\d{3}$/.test(f)) return 'Envoy Air';
          if (a.includes('alaska')   && /^3\d{3}$/.test(f))     return 'SkyWest';
          return '';
        }

        function routeForCallsign(cs) {
          if (!cs) return null;
          const norm = cs.replace(/\s+/g,'').toUpperCase();
          const digs = norm.replace(/[A-Z]/g,'');
          for (const f of arrivalRows()) {
            for (const c of candidates(f.airline, f.flight)) {
              const cc = c.replace(/\s+/g,'').toUpperCase();
              if (cc === norm || (digs && cc.replace(/[A-Z]/g,'') === digs))
                return {
                  from: airportCode(f.from)||f.from||'',
                  to: 'JAC',
                  airline: f.airline || airlineFromCallsign(cs),
                  flight: f.flight || flightNumberFromCallsign(cs),
                  operator: operatorFromCallsign(cs) || operatingCarrier(f.airline, f.flight),
                  confirmed: true
                };
            }
          }
          return null;
        }
        function landingIntent(dist, alt, vr, speed, heading, route, lat, lon) {
          if (route?.confirmed) return { label: 'Landing JAC', cls: 'confirmed' };
          const targetBearing = heading != null && lat != null && lon != null ? bearingTo(lat, lon, JAC_LAT, JAC_LON) : null;
          const aligned = heading != null && targetBearing != null && angleDiff(heading, targetBearing) <= 55;
          if (dist <= 8 || (dist <= 18 && alt <= 7000 && aligned)) return { label: 'Likely JAC', cls: 'likely' };
          if (dist <= 35 && aligned && (vr < -64 || speed < 230)) return { label: 'Possible JAC', cls: 'possible' };
          return { label: 'Nearby traffic', cls: 'nearby' };
        }

        // ─────────────────────────────────────────────────────────────
        // DETECT & RENDER INBOUND AIRCRAFT PANEL
        // ─────────────────────────────────────────────────────────────
        function detectInbound(states) {
          const cfg  = getSettings();
          const maxD = cfg.distance || 80;
          const list = [];
          for (const s of states) {
            const lat=s[6], lon=s[5]; if (!lat||!lon) continue;
            const alt = Math.round((s[13]||s[7]||0)*3.28084);
            const spd = Math.round((s[9]||0)*1.94384);
            const vr  = s[11]||0;
            const heading = s[10] != null ? Math.round(s[10]) : null;
            const dist= haversine(lat,lon,JAC_LAT,JAC_LON);
            if (dist>maxD || vr>800 || (spd<30&&dist>2)) continue;
            const callsign = (s[1]||'').trim();
            list.push({
              callsign,
              airline: airlineFromCallsign(callsign),
              flight: flightNumberFromCallsign(callsign),
              alt, spd, dist:Math.round(dist), vr, heading, lat, lon
            });
          }
          return list.sort((a,b) => a.dist - b.dist);
        }
        function confLevel(dist, alt, vr, speed) {
          const eta = etaMins(dist, speed);

          if (eta != null && eta <= 5) {
            return { p: 97, l: 'High', c: 'high' };
          }

          if (dist <= 6 || (eta != null && eta <= 8 && alt <= 5000)) {
            return { p: 92, l: 'High', c: 'high' };
          }

          if (dist <= 15 || (eta != null && eta <= 12 && alt <= 8000)) {
            return { p: 82, l: 'High', c: 'high' };
          }

          if ((dist <= 25 && alt <= 10000 && vr < -64) || (eta != null && eta <= 18)) {
            return { p: 68, l: 'Medium', c: 'mid' };
          }

          if (dist <= 45 || (eta != null && eta <= 30)) {
            return { p: 50, l: 'Medium', c: 'mid' };
          }

          if (dist <= 80 && alt <= 22000) {
            return { p: 34, l: 'Low', c: 'low' };
          }

          return { p: 20, l: 'Low', c: 'low' };
        }

        function altClass(alt)  { return alt<4000?'alt-low':alt<9000?'alt-mid':'alt-high'; }
        function spdClass(spd)  { return spd<150?'spd-slow':spd<220?'spd-approach':'spd-fast'; }

        function confHtml(p, l, c, wide) {
          return `<span class="fs-conf-bar" style="${wide?'width:52px;height:5px;':''}">`
               + `<span class="fs-conf-fill ${c}" style="width:${p}%;"></span></span>`;
        }

        function renderInbound(list) {
          let el = document.getElementById('fs-inbound-aircraft');
          const cfg = getSettings();
          if (!list?.length || cfg.inbound === false) { if (el) el.innerHTML = ''; return; }
          if (!el) {
            el = document.createElement('div'); el.id = 'fs-inbound-aircraft';
            const sc = document.getElementById('fs-alerts-content');
            if (sc) {
              const a = sc.querySelector('#fs-live-arrivals-panel') || sc.querySelector('.fs-weather-banner');
              if (a) a.insertAdjacentElement('afterend', el);
              else sc.querySelector('.fs-sheet-header-wrap')?.insertAdjacentElement('afterend', el) || sc.appendChild(el);
            } else document.body.appendChild(el);
          }
          const showConf  = cfg.confidence !== false;
          const showColor = cfg.colors !== false;
          const maxD = cfg.distance || 80;
          el.innerHTML = `<div class="fs-inbound-panel">
            <div class="fs-inbound-header">
              <span class="fs-inbound-title">Inbound traffic</span>
              <span class="fs-inbound-subtitle">${list.length} aircraft within ${maxD} mi</span>
            </div>
            <div class="fs-inbound-list">
              ${list.slice(0,6).map(ac => {
                const eta  = etaMins(ac.dist, ac.spd);
                const cf   = confLevel(ac.dist, ac.alt, ac.vr, ac.spd);
                const route= routeForCallsign(ac.callsign);
                const intent = landingIntent(ac.dist, ac.alt, ac.vr, ac.spd, ac.heading, route, ac.lat, ac.lon);
                const airline = route?.airline || ac.airline || 'Unknown airline';
                const flight = route?.flight || ac.flight || ac.callsign || '';
                const routeText = route?.confirmed ? `${route.from} → ${route.to || 'JAC'}` : `${intent.label}${ac.heading != null ? ` • ${ac.heading}°` : ''}`;
                const operator = route?.operator || operatorFromCallsign(ac.callsign);
                const aC   = showColor ? altClass(ac.alt) : '';
                const sC   = showColor ? spdClass(ac.spd) : '';
                return `<div class="fs-inbound-card${ac.dist<10?' final':''}">
                  <div class="fs-inbound-main">
                    <div class="fs-inbound-line1">
                      <span class="fs-inbound-icon">✈</span>
                      <span class="fs-inbound-callsign">${escHtml(airline)}${flight ? ` ${escHtml(flight)}` : ''}</span>
                      <span class="fs-inbound-route">${escHtml(routeText)}</span>
                      <span class="fs-landing-pill ${intent.cls}">${escHtml(intent.label)}</span>
                      ${eta?`<span class="fs-inbound-eta">ETA ${eta} min</span>`:''}
                    </div>
                    <div class="fs-inbound-line2">
                      <span>${escHtml(ac.callsign||'No callsign')}</span>
                      ${operator?`<span>• ${escHtml(operator)}</span>`:''}
                      <span class="fs-inbound-dist">${ac.dist} mi</span>
                      <span class="fs-inbound-alt ${aC}">${ac.alt.toLocaleString()} ft</span>
                      <span class="fs-inbound-spd ${sC}">${ac.spd} kt</span>
                      ${showConf?`<span class="fs-inbound-conf">
                        Approach: <span class="fs-conf-word ${cf.c}">${cf.l}</span>
                        ${confHtml(cf.p,cf.l,cf.c,false)}
                      </span>`:''}
                    </div>
                  </div>
                </div>`;
              }).join('')}
            </div>
          </div>`;
        }

        // ─────────────────────────────────────────────────────────────
        // DOM SCRAPE — cached, invalidated by MutationObserver
        // ─────────────────────────────────────────────────────────────
        function arrivalRows() {
          if (!scrapeDirty && scrapeCache) return scrapeCache;
          applyFlightRowStatusClasses();
          const con = document.getElementById('flight-container'); if (!con) return [];
          const rows = [];
          const txt = (row, selector, index) => {
            const el = row.querySelector(selector);
            if (el) return el.innerText.trim();
            const cells = row.querySelectorAll('td');
            return cells[index]?.innerText.trim() || '';
          };
          const tableType = table => {
            const wrap = table.closest('.-arrival, .-departure, [class*="arrival"], [class*="departure"]');
            const wrapClass = (wrap?.className || '').toString().toLowerCase();
            if (wrapClass.includes('departure')) return 'departure';
            if (wrapClass.includes('arrival')) return 'arrival';
            const heads = [...table.querySelectorAll('th')].map(th => th.innerText.trim().toLowerCase());
            if (heads.includes('from')) return 'arrival';
            if (heads.includes('to')) return 'departure';
            const prevText = (table.previousElementSibling?.innerText || table.parentElement?.previousElementSibling?.innerText || '').toLowerCase();
            if (prevText.includes('departure')) return 'departure';
            if (prevText.includes('arrival')) return 'arrival';
            return '';
          };
          con.querySelectorAll('table.jha-flights').forEach(table => {
            if (tableType(table) !== 'arrival') return;
            let curDate = '', todayLabel = '';
            table.querySelectorAll('tbody tr').forEach(row => {
              const dc = row.querySelector('.day');
              if (dc) { curDate = cleanDayText(dc); if (!todayLabel) todayLabel = curDate; return; }
              if (!todayLabel) todayLabel = curDate;
              const an = row.querySelector('.airline'), fn = row.querySelector('.flight');
              const cells = row.querySelectorAll('td');
              const airline = an?.innerText.trim() || cells[0]?.innerText.trim() || '';
              const flight = fn?.innerText.trim() || cells[1]?.innerText.trim() || '';
              if (!airline || !flight) return;
              const statusEl = row.querySelector('.status span') || row.querySelector('.status');
              let icon = '';
              const img = an?.querySelector('img'); if (img) icon = encodeURIComponent(img.outerHTML);
              rows.push({
                airline: airline, airlineIcon: icon,
                flight:  flight,
                sched:   txt(row, '.sched', 3),
                actual:  txt(row, '.actual', 4),
                from:    txt(row, '.from', 2),
                status:  statusEl?.textContent.trim() || txt(row, '.status', 5) || 'Scheduled'
              });
            });
          });
          scrapeCache = rows; scrapeDirty = false; return rows;
        }

        function scrapeAll() {
          applyFlightRowStatusClasses();
          const con = document.getElementById('flight-container'); if (!con) return null;
          const wx  = getWx();
          const luEl= document.querySelector('.flight-table__time');
          const normalizeLastUpdate = text => {
            const cleaned = (text || '').replace(/\s+/g, ' ').trim()
              .replace(/^last\s*update\s*:?\s*/i, '')
              .replace(/^last\s*updated\s*:?\s*/i, '')
              .replace(/^updated\s*:?\s*/i, '')
              .trim();
            return cleaned ? `Updated ${cleaned}` : '';
          };
          const lastUpdate = normalizeLastUpdate(luEl ? luEl.textContent : '');
          let arrAlerts=[], depAlerts=[];
          let totArr=0,totDep=0,totArrTm=0,totDepTm=0;
          let todayLbl='', tomorrowLbl='';
          const tableType = table => {
            const wrap = table.closest('.-arrival, .-departure, [class*="arrival"], [class*="departure"]');
            const wrapClass = (wrap?.className || '').toString().toLowerCase();
            if (wrapClass.includes('departure')) return 'departure';
            if (wrapClass.includes('arrival')) return 'arrival';
            const heads = [...table.querySelectorAll('th')].map(th => th.innerText.trim().toLowerCase());
            if (heads.includes('from')) return 'arrival';
            if (heads.includes('to')) return 'departure';
            return '';
          };
          con.querySelectorAll('table.jha-flights').forEach(table => {
            const type = tableType(table);
            const isArr= type === 'arrival';
            const isDep= type === 'departure';
            let curDate = '';
            table.querySelectorAll('tbody tr').forEach(row => {
              const dc = row.querySelector('.day');
              if (dc) {
                curDate = cleanDayText(dc);
                if (!todayLbl) todayLbl = curDate;
                else if (!tomorrowLbl && curDate !== todayLbl) tomorrowLbl = curDate;
                return;
              }
              const sp = row.querySelector('.status span') || row.querySelector('.status'); if (!sp) return;
              const sl = sp.textContent.trim().toLowerCase();
              const isDelay = sl.includes('delay'), isDiverted = sl.includes('divert'), isCancelled = sl.includes('cancel');
              const an = row.querySelector('.airline'), fn = row.querySelector('.flight');
              const airline = an?.innerText.trim()||'';
              let icon = ''; const img = an?.querySelector('img'); if (img) icon = img.outerHTML;
              const flight = fn?.innerText||'';
              const sched  = row.querySelector('.sched')?.innerText||'';
              const actual = row.querySelector('.actual')?.innerText||'';
              const from   = row.querySelector('.from')?.innerText||'';
              const code   = AIRPORT_CODES[from]||'';
              if (curDate===todayLbl)    { if(isArr)totArr++;  if(isDep)totDep++;  }
              if (curDate===tomorrowLbl) { if(isArr)totArrTm++; if(isDep)totDepTm++; }
              if (!isDelay&&!isDiverted&&!isCancelled) return;
              const dm = isCancelled ? 0 : delayMins(sched,actual);
              const reason = isCancelled ? 'Flight cancelled by airline'
                : isDiverted ? 'Flight diverted to alternate airport'
                : smartReason(dm, airline, flight, wx);
              let html = `<div class="fs-alert-date">${curDate}</div>
                <div class="fs-flight-row">
                  ${icon?`<span class="fs-airline-icon">${icon}</span>`:''}
                  <span class="fs-flight-name">${airline} ${flight}</span>
                  ${code?`<span class="fs-airport-code">${code}</span>`:''}`;
              if (isCancelled) {
                html += `<span class="status-word cancelled-word">CANCELLED</span></div>
                  <div class="fs-alert-times"><span class="sched-time">Sched: ${sched}</span></div>`;
              } else if (isDiverted) {
                html += `<span class="status-word diverted-word">
                  <svg xmlns="http://www.w3.org/2000/svg" height="14" viewBox="0 -960 960 960" width="14" fill="currentColor"><path d="M320-120q-66 0-113-47t-47-113q0-66 47-113t113-47h200q33 0 56.5-23.5T600-520q0-33-23.5-56.5T520-600H280v80L160-640l120-120v80h240q66 0 113 47t47 113q0 66-47 113t-113 47H320q-33 0-56.5 23.5T240-280q0 33 23.5 56.5T320-200h360v-80l120 120-120 120v-80H320Z"/></svg>
                  DIVERTED</span></div>`;
              } else {
                html += `<span class="status-word delayed-word">DELAYED</span>
                  <span class="fs-delay-time">${fmtDelay(dm)}</span>
                  <span class="fs-plus-format">(+${dm} min)</span></div>
                  <div class="fs-alert-times">
                    <span class="sched-time">Sched: ${sched}</span>
                    <span class="dot"> • </span>
                    <span class="actual-time">Actual: ${actual}</span>
                  </div>`;
              }
              html += `<div class="fs-delay-reason">${reason}</div>`;
              const obj = { html, delay:dm, diverted:isDiverted, cancelled:isCancelled };
              if (isArr) arrAlerts.push(obj);
              if (isDep) depAlerts.push(obj);
            });
          });
          const sort = list => list.sort((a,b) =>
            a.cancelled!==b.cancelled ? (a.cancelled?-1:1) :
            a.diverted!==b.diverted   ? (a.diverted?-1:1)  : b.delay-a.delay
          );
          return {
            arrAlerts: sort(arrAlerts), depAlerts: sort(depAlerts),
            totArr, totDep, totArrTm, totDepTm,
            todayLbl, tomorrowLbl, lastUpdate, wx
          };
        }

        function flightTableKind(table) {
          const wrap = table?.closest?.('.-arrival, .-departure, [class*="arrival"], [class*="departure"]');
          const wrapClass = (wrap?.className || '').toString().toLowerCase();
          if (wrapClass.includes('departure')) return 'departure';
          if (wrapClass.includes('arrival')) return 'arrival';
          const heads = [...(table?.querySelectorAll?.('th') || [])].map(th => th.innerText.trim().toLowerCase());
          if (heads.includes('to')) return 'departure';
          if (heads.includes('from')) return 'arrival';
          const prevText = (table?.previousElementSibling?.innerText || table?.parentElement?.previousElementSibling?.innerText || '').toLowerCase();
          if (prevText.includes('departure')) return 'departure';
          if (prevText.includes('arrival')) return 'arrival';
          return 'arrival';
        }

        function textFromCell(row, selector, index) {
          const el = row.querySelector(selector);
          if (el) return el.innerText.replace(/\s+/g, ' ').trim();
          const cells = row.querySelectorAll('td');
          return (cells[index]?.innerText || '').replace(/\s+/g, ' ').trim();
        }

        function rowFlightData(row) {
          if (!row || row.querySelector('.day') || row.querySelector('th')) return null;
          const table = row.closest('table.jha-flights');
          const kind = flightTableKind(table);
          const airline = textFromCell(row, '.airline', 0);
          const flight = textFromCell(row, '.flight', 1);
          if (!airline || !flight) return null;
          const statusEl = row.querySelector('.status span') || row.querySelector('.status');
          let day = '';
          let prev = row.previousElementSibling;
          while (prev) {
            const dc = prev.querySelector?.('.day');
            if (dc) { day = cleanDayText(dc); break; }
            prev = prev.previousElementSibling;
          }
          const place = textFromCell(row, kind === 'departure' ? '.to' : '.from', 2);
          return {
            row, kind, day,
            airline,
            flight,
            place,
            placeLabel: kind === 'departure' ? 'To' : 'From',
            route: kind === 'departure' ? `JAC to ${place || 'destination'}` : `${place || 'origin'} to JAC`,
            sched: textFromCell(row, '.sched', 3),
            actual: textFromCell(row, '.actual', 4),
            status: (statusEl?.textContent || textFromCell(row, '.status', 5) || 'Scheduled').trim()
          };
        }

        function statusTone(status) {
          const s = (status || '').toLowerCase();
          if (s.includes('cancel')) return 'cancelled';
          if (s.includes('divert')) return 'diverted';
          if (s.includes('delay')) return 'delayed';
          if (s.includes('arriv')) return 'arrived';
          return 'ontime';
        }

        function airlineCodes(airline) {
          const a = (airline || '').toLowerCase();
          if (a.includes('united')) return { iata:'UA', icao:'UAL' };
          if (a.includes('delta')) return { iata:'DL', icao:'DAL' };
          if (a.includes('american')) return { iata:'AA', icao:'AAL' };
          if (a.includes('alaska')) return { iata:'AS', icao:'ASA' };
          if (a.includes('southwest')) return { iata:'WN', icao:'SWA' };
          if (a.includes('jetblue')) return { iata:'B6', icao:'JBU' };
          if (a.includes('frontier')) return { iata:'F9', icao:'FFT' };
          if (a.includes('spirit')) return { iata:'NK', icao:'NKS' };
          if (a.includes('skywest')) return { iata:'OO', icao:'SKW' };
          return { iata:'', icao:'' };
        }

        function flightLookupTokens(f) {
          const raw = (f.flight || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
          const digits = raw.replace(/^[A-Z]{2,3}/, '').replace(/[A-Z]/g, '');
          const codes = airlineCodes(f.airline);
          const iata = /^[A-Z]/.test(raw) ? raw : (codes.iata && digits ? `${codes.iata}${digits}` : raw);
          const icao = codes.icao && digits ? `${codes.icao}${digits}` : iata;
          return { iata, icao };
        }

        function flightTimingSummary(f) {
          const tone = statusTone(f.status);
          if (tone === 'cancelled') return 'Cancelled by airline or airport operations';
          if (tone === 'diverted') return 'Diverted from the scheduled arrival plan';
          const diff = signedTimeDiffMins(f.sched, f.actual);
          if (diff == null) return 'No time comparison available yet';
          if (diff === 0) return 'On schedule';
          return tone === 'arrived' ? `Arrived ${fmtSignedMins(diff)}` : `Currently ${fmtSignedMins(diff)}`;
        }

        function closeFlightDetailDialog() {
          const overlay = document.getElementById('fs-flight-detail-overlay');
          if (overlay) {
            try { overlay._fsCleanup?.(); } catch {}
            overlay.remove();
          }
          document.documentElement.classList.remove('fs-flight-detail-open');
          if (window.fsPositionFlightDetail === overlay?._fsRelayout) {
            window.fsPositionFlightDetail = null;
          }
        }

        function liveFlightMarkup(ac, f) {
          if (!ac) {
            return `<div class="fs-detail-live-empty">
              No live aircraft match right now. Arrived flights often disappear from ADS-B shortly after landing.
            </div>`;
          }
          const dist = haversine(ac.lat, ac.lon, JAC_LAT, JAC_LON);
          const eta = etaMins(dist, ac.speed);
          const op = operatorFromCallsign(ac.callsign) || operatingCarrier(f.airline, f.flight);
          return `<div class="fs-detail-live-grid">
            <span><b>${escHtml(ac.callsign || 'Unknown')}</b><em>Callsign</em></span>
            <span><b>${Math.round(dist)} mi</b><em>From JAC</em></span>
            <span><b>${ac.altitude.toLocaleString()} ft</b><em>Altitude</em></span>
            <span><b>${ac.speed} kt</b><em>Speed</em></span>
            <span><b>${ac.heading == null ? '--' : `${ac.heading} deg`}</b><em>Heading</em></span>
            <span><b>${eta == null ? '--' : `${eta} min`}</b><em>ETA</em></span>
            ${op ? `<span class="wide"><b>${escHtml(op)}</b><em>Operating carrier</em></span>` : ''}
          </div>`;
        }

        async function hydrateFlightDetailLive(f) {
          const live = document.getElementById('fs-flight-detail-live');
          if (!live) return;
          live.innerHTML = `<div class="fs-detail-live-loading">Searching live ADS-B near Jackson Hole...</div>`;
          let ac = null;
          try {
            ac = await matchCandidates(candidates(f.airline, f.flight));
          } catch {}
          if (!document.getElementById('fs-flight-detail-live')) return;
          live.innerHTML = liveFlightMarkup(ac, f);
          try {
            window.fsPositionFlightDetail?.();
            requestAnimationFrame(() => window.fsPositionFlightDetail?.());
          } catch {}
        }

        function positionFlightDetailPopover(f, overlay) {
          const card = overlay?.querySelector?.('.fs-flight-detail-card');
          const row = f?.row;
          if (!card || !row) return;
          const airlineAnchor = row.querySelector('td.airline .fs-cell-chip, .airline .fs-cell-chip, td.airline, .airline');
          const rect = (airlineAnchor || row).getBoundingClientRect();
          const margin = 10;
          const bottomReserve = Math.max(96, (document.getElementById('fs-bottom-tabs')?.offsetHeight || 0) + 22);
          const width = Math.min(300, Math.max(252, window.innerWidth - margin * 2));
          card.style.setProperty('width', `${width}px`, 'important');
          card.style.setProperty('min-height', '292px', 'important');
          card.style.setProperty('max-height', `${Math.max(292, Math.min(430, window.innerHeight - bottomReserve - margin * 2))}px`, 'important');
          card.style.setProperty('height', 'auto', 'important');
          card.style.setProperty('left', '0px', 'important');
          card.style.setProperty('top', '0px', 'important');
          card.classList.remove('above');
          const measured = card.getBoundingClientRect();
          const height = Math.min(measured.height || 300, Math.max(240, window.innerHeight - margin * 2));
          const anchorX = rect.left + rect.width / 2;
          let left = Math.min(Math.max(rect.left, margin), window.innerWidth - width - margin);
          if (window.innerWidth - width - margin < margin) left = margin;
          let top = rect.bottom + 10;
          const maxBottom = window.innerHeight - bottomReserve;
          let above = false;
          if (top + height > maxBottom && rect.top - height - 10 >= margin) {
            top = rect.top - height - 10;
            above = true;
          } else if (top + height > window.innerHeight - margin) {
            top = Math.max(margin, Math.min(rect.bottom + 10, window.innerHeight - height - margin));
          }
          const arrow = Math.min(Math.max(anchorX - left, 28), width - 28);
          card.style.setProperty('left', `${Math.round(left)}px`, 'important');
          card.style.setProperty('top', `${Math.round(top)}px`, 'important');
          card.style.setProperty('--fs-detail-arrow-left', `${Math.round(arrow)}px`);
          if (above) card.classList.add('above');
        }

        function showFlightDetailDialog(f) {
          if (!f) return;
          closeFlightDetailDialog();
          const tone = statusTone(f.status);
          const tokens = flightLookupTokens(f);
          const fr24 = tokens.iata ? `https://www.flightradar24.com/data/flights/${tokens.iata.toLowerCase()}` : 'https://www.flightradar24.com/data';
          const aware = tokens.icao ? `https://www.flightaware.com/live/flight/${tokens.icao}` : 'https://www.flightaware.com/live/';
          const overlay = document.createElement('div');
          overlay.id = 'fs-flight-detail-overlay';
          overlay.innerHTML = `<div class="fs-flight-detail-card ${tone}" role="dialog" aria-modal="true" aria-label="Flight details">
            <button type="button" class="fs-detail-close" data-fs-detail-close aria-label="Close">x</button>
            <div class="fs-detail-kicker">${escHtml(f.kind === 'departure' ? 'Departure details' : 'Arrival details')}</div>
            <div class="fs-detail-title">
              <span>${escHtml(f.airline)} ${escHtml(f.flight)}</span>
              <span class="fs-detail-status ${tone}">${escHtml(f.status || 'Scheduled')}</span>
            </div>
            <div class="fs-detail-route">${escHtml(f.route)}</div>
            ${f.day ? `<div class="fs-detail-day">${escHtml(f.day)}</div>` : ''}
            <div class="fs-detail-summary">${escHtml(flightTimingSummary(f))}</div>
            <div class="fs-detail-metrics">
              <span><b>${escHtml(f.sched || '--')}</b><em>Scheduled</em></span>
              <span><b>${escHtml(f.actual || '--')}</b><em>Actual</em></span>
              <span><b>${escHtml(airportCode(f.place) || f.place || '--')}</b><em>${escHtml(f.placeLabel)}</em></span>
            </div>
            <div class="fs-detail-section-title">Live radar match</div>
            <div id="fs-flight-detail-live" class="fs-detail-live"></div>
            <div class="fs-detail-actions">
              <a href="${fr24}" target="_blank" rel="noopener">FlightRadar24</a>
              <a href="${aware}" target="_blank" rel="noopener">FlightAware</a>
            </div>
          </div>`;
          overlay.addEventListener('click', e => {
            if (e.target === overlay || e.target.closest('[data-fs-detail-close]')) closeFlightDetailDialog();
          });
          document.body.appendChild(overlay);
          document.documentElement.classList.add('fs-flight-detail-open');
          const relayout = () => positionFlightDetailPopover(f, overlay);
          overlay._fsRelayout = relayout;
          overlay._fsCleanup = () => {
            window.removeEventListener('scroll', relayout, true);
            window.removeEventListener('resize', relayout);
          };
          window.addEventListener('scroll', relayout, true);
          window.addEventListener('resize', relayout);
          window.fsPositionFlightDetail = relayout;
          relayout();
          requestAnimationFrame(relayout);
          hydrateFlightDetailLive(f);
          setTimeout(relayout, 180);
        }

        function bindFlightRowDetails() {
          const con = document.getElementById('flight-container');
          if (!con) return;
          con.querySelectorAll('table.jha-flights tbody tr').forEach(row => {
            if (row.dataset.fsDetailBound === '1' || row.querySelector('.day') || row.querySelector('th')) return;
            if (!rowFlightData(row)) return;
            row.dataset.fsDetailBound = '1';
            row.classList.add('fs-flight-detail-ready');
            let timer = null, startX = 0, startY = 0;
            const clear = () => {
              if (timer) clearTimeout(timer);
              timer = null;
              row.classList.remove('fs-flight-holding');
            };
            const point = ev => {
              const t = ev.touches?.[0] || ev.changedTouches?.[0] || ev;
              return { x: t.clientX || 0, y: t.clientY || 0 };
            };
            const start = ev => {
              if (ev.button != null && ev.button !== 0) return;
              if (ev.touches && ev.touches.length > 1) return;
              const p = point(ev);
              startX = p.x; startY = p.y;
              clear();
              row.classList.add('fs-flight-holding');
              timer = setTimeout(() => {
                const f = rowFlightData(row);
                clear();
                if (!f) return;
                try { navigator.vibrate?.(18); } catch {}
                try { window.getSelection?.().removeAllRanges?.(); } catch {}
                showFlightDetailDialog(f);
              }, 950);
            };
            const move = ev => {
              if (!timer) return;
              const p = point(ev);
              if (Math.abs(p.x - startX) > 12 || Math.abs(p.y - startY) > 12) clear();
            };
            row.addEventListener('touchstart', start, { passive: true });
            row.addEventListener('touchmove', move, { passive: true });
            row.addEventListener('touchend', clear, { passive: true });
            row.addEventListener('touchcancel', clear, { passive: true });
            row.addEventListener('mousedown', start);
            row.addEventListener('mousemove', move);
            row.addEventListener('mouseup', clear);
            row.addEventListener('mouseleave', clear);
            row.addEventListener('contextmenu', ev => {
              ev.preventDefault();
              clear();
              showFlightDetailDialog(rowFlightData(row));
            });
            row.addEventListener('selectstart', ev => ev.preventDefault());
          });
        }

        function smartReason(min, airline, flight, wx) {
          if (min>=20 && wx?.windSpeed>=20) return 'Strong crosswinds affecting operations';
          const seed = (airline+flight).length;
          if (min>=180) return 'Operational aircraft rotation disruption';
          if (min>=120) return ['Late inbound aircraft','Network traffic flow management','Aircraft repositioning delay'][seed%3];
          if (min>=60)  return ['Air traffic congestion','Crew scheduling adjustment','Gate availability delay'][seed%3];
          if (min>=30)  return 'Minor operational delay';
          return 'Schedule adjustment';
        }

        // ─────────────────────────────────────────────────────────────
        // BUILD ALERT CONTENT HTML (pure function, no DOM writes)
        // ─────────────────────────────────────────────────────────────
        function statusClass(a) {
          if (a.diverted)   return 'status-diverted';
          if (a.delay>=180) return 'status-major';
          if (a.delay>=60)  return 'status-medium';
          if (a.delay>0)    return 'status-minor';
          return '';
        }

        function chipsHtml(alerts, isArr) {
          const delayed    = alerts.filter(a => !a.cancelled&&!a.diverted&&a.delay>0).length;
          const cancelled  = alerts.filter(a => a.cancelled).length;
          const diverted   = alerts.filter(a => a.diverted).length;
          if (!delayed && !cancelled && !diverted) return '';
          const parts = [];
          if (delayed)   parts.push(`<span class="fs-chip fs-chip-delay"><svg width="10" height="10" viewBox="0 -960 960 960" fill="currentColor"><path d="M480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320 113 113-57 57-96-96v-200h80v126Z"/></svg>${delayed} Delayed</span>`);
          if (cancelled) parts.push(`<span class="fs-chip fs-chip-cancel"><svg width="10" height="10" viewBox="0 -960 960 960" fill="currentColor"><path d="M480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm-40-320v-240h80v240h-80Zm0 160v-80h80v80h-80Z"/></svg>${cancelled} Cancelled</span>`);
          if (diverted)  parts.push(`<span class="fs-chip fs-chip-divert"><svg width="10" height="10" viewBox="0 -960 960 960" fill="currentColor"><path d="M320-120q-66 0-113-47t-47-113q0-66 47-113t113-47h200q33 0 56.5-23.5T600-520q0-33-23.5-56.5T520-600H280v80L160-640l120-120v80h240q66 0 113 47t47 113q0 66-47 113t-113 47H320q-33 0-56.5 23.5T240-280q0 33 23.5 56.5T320-200h360v-80l120 120-120 120v-80H320Z"/></svg>${diverted} Diverted</span>`);
          return `<div class="fs-chips">${parts.join('')}</div>`;
        }

        function opsSummaryHtml(data) {
          const arrDelayed = data.arrAlerts.filter(a => !a.cancelled && !a.diverted && a.delay > 0).length;
          const depDelayed = data.depAlerts.filter(a => !a.cancelled && !a.diverted && a.delay > 0).length;
          const cancelled = [...data.arrAlerts, ...data.depAlerts].filter(a => a.cancelled).length;
          const diverted = [...data.arrAlerts, ...data.depAlerts].filter(a => a.diverted).length;
          const parts = [
            `<span class="fs-ops-pill fs-ops-arr"><b>${arrDelayed}</b> arrival delay${arrDelayed===1?'':'s'}</span>`,
            `<span class="fs-ops-pill fs-ops-dep"><b>${depDelayed}</b> departure delay${depDelayed===1?'':'s'}</span>`
          ];
          if (cancelled) parts.push(`<span class="fs-ops-pill fs-ops-critical"><b>${cancelled}</b> cancelled</span>`);
          if (diverted) parts.push(`<span class="fs-ops-pill fs-ops-critical"><b>${diverted}</b> diverted</span>`);
          if (!arrDelayed && !depDelayed && !cancelled && !diverted) {
            parts.push(`<span class="fs-ops-pill fs-ops-clear">On time</span>`);
          }
          if (data.lastUpdate) parts.push(`<span class="fs-ops-pill fs-ops-time">${data.lastUpdate}</span>`);
          return `<div class="fs-ops-summary">${parts.join('')}</div>`;
        }

        function collectAiFlightRows() {
          const con = document.getElementById('flight-container');
          if (!con) return [];
          const rows = [];
          con.querySelectorAll('table.jha-flights tbody tr').forEach(row => {
            const data = rowFlightData(row);
            if (!data) return;
            const tone = statusTone(data.status);
            const delay = tone === 'cancelled' || tone === 'diverted' ? 0 : delayMins(data.sched, data.actual);
            rows.push({
              kind: data.kind,
              day: data.day,
              airline: data.airline,
              flight: data.flight,
              place: data.place,
              route: data.route,
              sched: data.sched,
              actual: data.actual,
              status: data.status || 'Scheduled',
              tone,
              delay
            });
          });
          return rows;
        }

        function aiSummaryLine(rows) {
          const delayedRows = rows.filter(f => f.tone === 'delayed' || f.delay > 0);
          const delayed = delayedRows.length;
          const cancelled = rows.filter(f => f.tone === 'cancelled').length;
          const diverted = rows.filter(f => f.tone === 'diverted').length;
          if (!rows.length) return 'Flight table is still loading.';
          const days = [];
          rows.forEach(f => {
            const key = f.day || 'Today';
            let d = days.find(item => item.key === key);
            if (!d) {
              d = { key, arr: 0, dep: 0 };
              days.push(d);
            }
            if (f.kind === 'departure') d.dep += 1; else d.arr += 1;
          });
          const dayText = days.slice(0, 2).map((d, index) => {
            const label = d.key || (index === 0 ? 'Today' : 'Tomorrow');
            return `${label}: ${d.arr} arrival${d.arr === 1 ? '' : 's'}, ${d.dep} departure${d.dep === 1 ? '' : 's'}`;
          }).join('. ');
          const longestDelay = delayedRows.reduce((max, f) => Math.max(max, f.delay || 0), 0);
          const delayTail = longestDelay > 0 ? ` Longest visible delay is ${longestDelay} min.` : '';
          const issueText = (!delayed && !cancelled && !diverted)
            ? 'No delays, cancellations, or diversions visible right now.'
            : `${delayed} delayed, ${cancelled} cancelled, ${diverted} diverted.${delayTail}`;
          return `${dayText}. ${issueText}`;
        }

        function aiFlightSnapshot(rows) {
          const firstDay = rows[0]?.day || 'Today';
          const briefingRows = rows.filter(f => (f.day || 'Today') === firstDay);
          const sourceRows = briefingRows.length ? briefingRows : rows;
          const issues = sourceRows
            .filter(f => f.tone === 'cancelled' || f.tone === 'diverted' || f.tone === 'delayed' || f.delay > 0)
            .sort((a, b) => {
              const rank = f => f.tone === 'cancelled' ? 3 : f.tone === 'diverted' ? 2 : 1;
              return rank(b) - rank(a) || (b.delay || 0) - (a.delay || 0);
            });
          const delayedRows = sourceRows.filter(f => f.tone === 'delayed' || f.delay > 0);
          return {
            summary: aiSummaryLine(sourceRows),
            issueCount: issues.length,
            issues: issues.slice(0, 4).map(f => {
              const label = f.tone === 'cancelled' ? 'Cancelled'
                : f.tone === 'diverted' ? 'Diverted'
                : f.delay > 0 ? `+${f.delay} min`
                : (f.status || 'Delayed');
              const time = f.actual && f.actual !== f.sched ? `${f.sched} → ${f.actual}` : (f.sched || 'time pending');
              return {
                label,
                flight: `${f.airline} ${f.flight}`.trim(),
                route: f.route || '',
                time,
                tone: f.tone || ''
              };
            }),
            arrivalCount: sourceRows.filter(f => f.kind !== 'departure').length,
            departureCount: sourceRows.filter(f => f.kind === 'departure').length,
            delayedCount: delayedRows.length,
            cancelledCount: sourceRows.filter(f => f.tone === 'cancelled').length,
            divertedCount: sourceRows.filter(f => f.tone === 'diverted').length,
            source: 'webview_table',
            updatedAt: Date.now()
          };
        }

        function syncAiBriefSnapshot() {
          try {
            if (!window.FlightsAndroidBridge?.updateFlightBriefSnapshot) return;
            const rows = collectAiFlightRows();
            if (!rows.length) return;
            window.FlightsAndroidBridge.updateFlightBriefSnapshot(JSON.stringify(aiFlightSnapshot(rows)));
            syncWeatherSnapshot();
          } catch (e) {}
        }

        function dockIconSvg() {
          const docked = getSettings().dock === true;
          return docked
            ? `<svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-280h560v-360H200v360Z"/></svg>`
            : `<svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M256-215 215-256l224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z"/></svg>`;
        }

        function buildContentHtml(data, liveHtml) {
          const cfg = getSettings();
          const { arrAlerts, depAlerts, totArr, totDep, totArrTm, totDepTm, todayLbl, tomorrowLbl, lastUpdate, wx } = data;
          const tC   = tempColor(wx.tempF);
          const warn = cfg.weather!==false ? wxWarnings(wx) : '';
          const out  = [];
          // ── header bar
          out.push(`<div class="fs-sheet-header-wrap">
            <div class="fs-sheet-header">
              <div class="fs-sheet-title-block">
                <span class="fs-sheet-title">Flight Intelligence</span>
                <span class="fs-built-by">◉ NeuroAI</span>
              </div>
              <div class="fs-sheet-divider"></div>
              <div class="fs-sheet-actions">
                <button id="fs-close-btn" class="fs-sheet-action" aria-label="Close">
                  ${dockIconSvg()}
                </button>
                <button id="fs-settings-btn" class="fs-sheet-action" aria-label="Settings">
                  <svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-13.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l119-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24 12L590-80H370Zm112-260q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Z"/></svg>
                </button>
              </div>
            </div>
          </div>`);
          out.push(`<div class="fs-sheet-preview-tools">
            <div class="fs-sheet-drag-handle" aria-hidden="true"></div>
            <button id="fs-sheet-menu-btn" class="fs-sheet-menu-btn" aria-label="Menu">
              <svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z"/></svg>
            </button>
          </div>`);
          out.push(opsSummaryHtml(data));
          // ── weather
          if (wx.temp) out.push(`<div class="fs-weather-banner ${wxClass(wx)}" style="--tempColor:${tC}">
            <span class="fs-weather-icon">${wx.icon}</span>
            <span class="fs-weather-text">
              <span class="wx-temp">${wx.temp}</span>
              ${wx.windSpeed?`<span class="wx-wind"> • Wind ${wx.windSpeed} ${wx.windUnit||'mph'} ${wx.windDir||''}</span>`:''}
              ${wx.vis!=null?`<span class="wx-vis"> • Vis ${wx.vis} mi</span>`:''}
              ${wx.cloud?`<span class="wx-cloud"> • CC ${wx.cloud}%</span>`:''}
              ${warn?`<span class="wx-warning">${warn}</span>`:''}
            </span>
          </div>`);
          // ── live arrivals slot
          if (liveHtml) out.push(liveHtml);
          // ── arrivals alerts
          if (arrAlerts.length) out.push(`<div class="fs-alert-section arrival-alert">
            <div class="fs-section-title">🔴 Arrivals</div>
            ${chipsHtml(arrAlerts,true)}
            ${arrAlerts.map(a=>`<div class="fs-alert-item ${statusClass(a)}">${a.html}</div>`).join('')}
          </div>`);
          // ── departures alerts
          if (depAlerts.length) out.push(`<div class="fs-alert-section departure-alert">
            <div class="fs-section-title">🔵 Departures</div>
            ${chipsHtml(depAlerts,false)}
            ${depAlerts.map(a=>`<div class="fs-alert-item ${statusClass(a)}">${a.html}</div>`).join('')}
          </div>`);
          // ── all-clear
          if (!arrAlerts.length && !depAlerts.length) {
            const countRow = (icon, n, label) => n ? `<span class="fs-count-icon">
              ${icon}<span>${n} ${label}</span>
            </span>` : '';
            const arrSvg = `<svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="M120-120v-80h720v80H120Zm622-202L120-499v-291l96 27 48 139 138 39-35-343 115 34 128 369 172 49q25 8 41.5 29t16.5 48q0 35-28.5 61.5T742-322Z"/></svg>`;
            const depSvg = `<svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="M120-120v-80h720v80H120Zm70-200L40-570l96-26 112 94 140-37-207-276 116-31 299 251 170-46q32-9 60.5 7.5T864-585q9 32-7.5 60.5T808-487L190-320Z"/></svg>`;
            out.push(`<div class="fs-alert success">
              <div class="fs-success-main">✓ All flights on time</div>
              ${todayLbl?`<div class="fs-success-date">${todayLbl}</div>
                <div class="fs-success-counts">
                  ${countRow(arrSvg,totArr,'arrivals')}
                  ${totArr&&totDep?'<span>•</span>':''}
                  ${countRow(depSvg,totDep,'departures')}
                </div>`:''}
              ${tomorrowLbl?`<div class="fs-success-date">${tomorrowLbl}</div>
                <div class="fs-success-counts">
                  ${countRow(arrSvg,totArrTm,'arrivals')}
                  ${totArrTm&&totDepTm?'<span>•</span>':''}
                  ${countRow(depSvg,totDepTm,'departures')}
                </div>`:''}
            </div>`);
          }
          return out.join('');
        }
        
        // ─────────────────────────────────────────────────────────────
        // Source badge
        // ─────────────────────────────────────────────────────────────
        function primaryBadge(state, hasLive, dist, alt, vr, speed) {
          if (state === 'landed') return { text: 'ARRIVED', cls: 'arrived' };
          if (!hasLive) return { text: 'SCHEDULE', cls: 'schedule' };

          const eta = etaMins(dist, speed);

          if (eta != null && eta <= 5) return { text: 'FINAL', cls: 'final' };
          if (dist <= 8 && alt <= 4000) return { text: 'FINAL', cls: 'final' };

          if ((dist <= 20 && alt <= 8000 && vr < -64) || (eta != null && eta <= 12)) {
            return { text: 'APPROACH', cls: 'approach' };
          }

          if (vr < -64) return { text: 'DESCENDING', cls: 'descending' };

          return { text: 'EN ROUTE', cls: 'enroute' };
        }

        function phaseBadge(dist, alt, vr, speed, state) {
          if (state === 'landed') return { text: 'LANDED', cls: 'landed' };
          const eta = etaMins(dist, speed);

          if (eta != null && eta <= 5) return { text: 'FINAL', cls: 'final' };
          if (dist <= 8 && alt <= 4000) return { text: 'FINAL', cls: 'final' };
          if ((dist <= 20 && alt <= 8000 && vr < -64) || (eta != null && eta <= 12)) {
            return { text: 'APPROACH', cls: 'approach' };
          }
          if (vr < -64) return { text: 'DESCENDING', cls: 'descending' };
          return { text: 'EN ROUTE', cls: 'enroute' };
        }

        function verticalArrow(vr) {
          if (vr < -64) return '↘';
          if (vr > 64) return '↗';
          return '→';
        }

        function delaySeverity(delayMin) {
          if (!delayMin || delayMin <= 0) return null;
          if (delayMin >= 120) return { text: 'SEVERE DELAY', cls: 'severe' };
          if (delayMin >= 45) return { text: 'MAJOR DELAY', cls: 'major' };
          return { text: 'MINOR DELAY', cls: 'minor' };
        }

        function confidenceReason(dist, alt, vr, speed, hasLive) {
          if (!hasLive) return 'Schedule-based only';
          const eta = etaMins(dist, speed);
          if (eta != null && eta <= 5) return 'ETA under 5 min';
          if (dist <= 10 && alt <= 5000 && vr < -64) return 'Close + descending';
          if (dist <= 20 && vr < -64) return 'Inbound descent detected';
          return 'Live track matched';
        }
        
        

        // ─────────────────────────────────────────────────────────────
        // LIVE ARRIVALS CARDS
        // ─────────────────────────────────────────────────────────────
        function liveIconHtml(encoded) { return encoded ? decodeURIComponent(encoded) : ''; }

        function titleRow(iconHtml, airline, flight) {
          return `<span class="fs-live-flight-row">
            ${iconHtml?`<span class="fs-live-icon">${iconHtml}</span>`:''}
            <span class="fs-live-name">${airline} ${flight}</span>
          </span>`;
        }
        function trackedCardInner(ac, airlineIcon, airline, flight, from, depGate, arrGate, operator, initDist) {
          const dist = haversine(ac.lat, ac.lon, JAC_LAT, JAC_LON);
          const cfg  = getSettings();
          const eta  = etaMins(dist, ac.speed);
          const pct  = Math.max(0, Math.min(100, ((Math.max(dist, initDist || 0, 50) - dist) / Math.max(dist, initDist || 0, 50)) * 100));
          const cf   = confLevel(dist, ac.altitude, ac.vRate, ac.speed);
          const aC   = cfg.colors !== false ? altClass(ac.altitude) : '';
          const sC   = cfg.colors !== false ? spdClass(ac.speed) : '';
          const from2= airportCode(from) || from || '';
          const via  = [from2, depGate ? `Gate ${depGate}` : ''].filter(Boolean).join(' ');
          const route= `${via} → JAC${arrGate ? ` Gate ${arrGate}` : ''}${operator ? ` · ${operator}` : ''}`;
          const badge = primaryBadge('inbound', true, dist, ac.altitude, ac.vRate, ac.speed);
          const trend = verticalArrow(ac.vRate);

          const icon = liveIconHtml(airlineIcon);
          return `<div class="fs-live-line1">
    ${titleRow(icon, airline, flight)}
    <span class="fs-live-badges">
      <span class="fs-badge fs-badge-${badge.cls}">${badge.text}</span>
      <span class="fs-landing-pill confirmed">Landing JAC</span>
    </span>
  </div>
  <div class="fs-live-line2 strong"><span>En route to JAC • ${route}</span></div>
  ${operator ? `<div class="fs-live-line3">Airline: ${escHtml(airline || airlineFromCallsign(ac.rawCallsign) || 'Unknown')} • Operated by ${escHtml(operator)}</div>` : `<div class="fs-live-line3">Airline: ${escHtml(airline || airlineFromCallsign(ac.rawCallsign) || 'Unknown')}</div>`}

  <div class="fs-live-line2">
    <span class="fs-live-dist">${Math.round(dist)} mi</span>
    <span>•</span>
    <span class="fs-live-alt ${aC}" data-alt="${ac.altitude}">${trend} ${ac.altitude.toLocaleString()} ft</span>
    <span>•</span>
    <span class="fs-live-spd ${sC}" data-spd="${ac.speed}">${ac.speed} kt</span>
    ${eta ? `<span class="fs-live-eta">• ETA ${eta} min</span>` : ''}
  </div>
  ${cfg.confidence !== false ? `<div class="fs-conf-row">
    <span class="fs-conf-label">Approach: <span class="fs-conf-word ${cf.c}">${cf.l}</span></span>
    ${confHtml(cf.p, cf.l, cf.c, true)}
  </div>
  <div class="fs-live-line3">${confidenceReason(dist, ac.altitude, ac.vRate, ac.speed, true)}</div>` : ''}
  <div class="fs-prog mt"><div class="fs-prog-bar" style="width:${pct}%;"></div></div>`;
        }
        
        function fallbackCardInner(airlineIcon, airline, flight, from, sched, actual, depGate, arrGate, operator) {
          const now  = new Date();
          const sDt  = clockToDate(sched), aDt = clockToDate(actual);
          const best = aDt || sDt;
          let eta = null;

          if (best) {
            eta = Math.round((best.getTime() - now.getTime()) / 60000);
            if (eta < -720) eta += 1440;
          }

          const dm   = delayMins(sched, actual);
          let status = 'Searching live track…', state = 'waiting';

          if (eta != null && eta > 2) {
            status = 'En route';
          } else if (eta != null && eta >= -15) {
            status = 'Arriving now';
            state = 'inbound';
          } else {
            status = acOk ? 'Searching live track…' : 'Live data loading…';
            state = 'waiting';
          }

          const from2 = airportCode(from) || from || '';
          const route = [from2, depGate ? `Gate ${depGate}` : ''].filter(Boolean).join(' ')
            + ' → JAC'
            + (arrGate ? ` Gate ${arrGate}` : '');

          const icon = liveIconHtml(airlineIcon);
          const pct  = eta != null && eta > 0 ? Math.max(3, Math.min(95, ((120 - eta) / 120) * 100)) : 0;
          const cfg  = getSettings();
          const badge = primaryBadge(state, false, 999, 99999, 0, 0);

          const sev = delaySeverity(dm);

          let cf;
          if (eta != null && eta <= 5) {
            cf = { p: 85, l: 'High', c: 'high' };
          } else if (eta != null && eta <= 15) {
            cf = { p: 65, l: 'Medium', c: 'mid' };
          } else {
            cf = { p: 30, l: 'Low', c: 'low' };
          }

          return {
            state,
            html: `
              <div class="fs-live-line1">
                ${titleRow(icon, airline, flight)}
                <span class="fs-live-badges">
                  <span class="fs-badge fs-badge-${badge.cls}">${badge.text}</span>
                  <span class="fs-landing-pill confirmed">Landing JAC</span>
                  ${sev ? `<span class="fs-badge fs-badge-${sev.cls}">${sev.text}</span>` : ''}
                </span>
                ${eta != null && eta > 0 && state !== 'landed' ? `<span class="fs-live-eta">${fmtMin(eta)} remaining</span>` : ''}
              </div>
              <div class="fs-live-line2 strong"><span>${status} to JAC • ${route}</span></div>
              <div class="fs-live-line3">Airline: ${escHtml(airline || 'Unknown')}${operator ? ` • Operated by ${escHtml(operator)}` : ''}</div>
              <div class="fs-live-line2">
                ${sched ? `<span>Sched ${sched}</span>` : ''}
                ${actual ? `<span>• Est ${actual}</span>` : ''}
                ${dm > 0 ? `<span>• +${fmtDelay(dm)}</span>` : ''}
              </div>
              ${cfg.confidence !== false ? `<div class="fs-conf-row">
                <span class="fs-conf-label">Approach: <span class="fs-conf-word ${cf.c}">${cf.l}</span></span>
                ${confHtml(cf.p, cf.l, cf.c, true)}
              </div>
              <div class="fs-live-line3">Schedule-based only</div>` : ''}
              <div class="fs-prog mt"><div class="fs-prog-bar${eta != null && eta > 0 ? '' : ' loading'}" style="width:${pct}%;"></div></div>`
          };
        }

        async function buildLivePanelHtml() {
          const cfg = getSettings();
          if (cfg.liveArrivals === false) return '';
          const rows = arrivalRows();
          const now  = new Date();
          const nowMins = now.getHours()*60+now.getMinutes();
          const inAir=[], landed=[];
          rows.forEach(f => {
            const sl = (f.status||'').toLowerCase();
            if (sl.includes('cancel')||sl.includes('divert')) return;
            if (sl.includes('arrived')) { if (recentLanding(f)) landed.push(f); return; }
            const sm = clockToMins(f.actual || f.sched); let mu = sm!=null?sm-nowMins:null;
            if (mu!=null&&mu<-30) mu+=1440;
            if (sm==null||(mu!=null&&mu<=180)) inAir.push(f);
          });
          if (!inAir.length && rows.length) {
            rows
              .filter(f => !/(cancel|divert|arrived)/i.test(f.status || ''))
              .map(f => {
                const tm = clockToMins(f.actual || f.sched);
                let mins = tm == null ? 9999 : tm - nowMins;
                if (mins < -30) mins += 1440;
                return { f, mins };
              })
              .filter(x => x.mins >= -30 && x.mins <= 720)
              .sort((a, b) => a.mins - b.mins)
              .slice(0, 4)
              .forEach(x => inAir.push(x.f));
          }
          refreshAcCache();
          const cards = await Promise.all(inAir.slice(0,6).map(async f => {
            const cands = candidates(f.airline,f.flight);
            let ac = null;
            if (acCache) {
              for (const c of cands) { ac = lookupCallsign(c); if (ac) break; }
              if (!ac) ac = fuzzyMatch(cands);
            }
            const op    = operatorFromCallsign(ac?.rawCallsign||'')||operatingCarrier(f.airline,f.flight);
            const dist  = ac ? haversine(ac.lat,ac.lon,JAC_LAT,JAC_LON) : 0;
            const dStr  = ac ? String(Math.round(dist)) : '';
            const fallback = ac ? null : fallbackCardInner(f.airlineIcon, f.airline, f.flight, f.from, f.sched, f.actual, '', '', op);
            const inner = ac
              ? trackedCardInner(ac,f.airlineIcon,f.airline,f.flight,f.from,'','',op,dist)
              : fallback.html;
            const state = ac ? 'inbound' : fallback.state;
            return `<div class="fs-live-card" data-state="${state}"
              data-flight="${f.flight}" data-airline="${f.airline}" data-icon="${f.airlineIcon||''}"
              data-from="${f.from}" data-sched="${f.sched}" data-actual="${f.actual}"
              data-dg="" data-ag="" data-op="${op}" data-init="${dStr}"
              ${ac?`data-cs="${ac.rawCallsign}"`:''}>${inner}</div>`;
          }));
          const landedCards = landed
            .sort((a,b)=>(minsAgo(a.actual||a.sched||'')||9999)-(minsAgo(b.actual||b.sched||'')||9999))
            .slice(0,3).map(f => {
              const ma = minsAgo(f.actual||f.sched||'');
              const icon= liveIconHtml(f.airlineIcon);
              const oc  = airportCode(f.from)||f.from||'';
              const lbl = ma!=null&&ma<1?'Just landed':`Landed ${ma} min ago`;
              return `<div class="fs-live-card fs-landed-card" data-state="landed" data-flight="${f.flight}">
                <div class="fs-landed-top">
                  <span class="fs-landed-flight">${titleRow(icon,f.airline,f.flight)}</span>
                  <span class="fs-landed-route">${oc?oc+' → JAC':'→ JAC'}</span>
                </div>
                <div class="fs-landed-bottom">
                <span class="fs-live-status">✓ Arrived</span>
                <span class="fs-badge fs-badge-arrived">ARRIVED</span>
                  <span>${lbl}</span>
                  ${f.actual?`<span>• ${f.actual}</span>`:''}
                </div>
              </div>`;
            });
          const all = [...cards, ...landedCards].filter(Boolean);
          const titleHtml = `<div class="fs-live-panel-title">Live arrival status
           <span style="display:flex;align-items:center;gap:6px;">
             <span class="fs-live-updated">${formatUpdatedLabel(false)}</span>
              <button class="fs-refresh-btn" id="fs-live-refresh" aria-label="Refresh">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 -960 960 960" fill="currentColor"><path d="M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-110h80v280H520v-80h168q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q77 0 139-44t87-116h84q-28 106-114 173t-196 67Z"/></svg>
              </button>
            </span>
          </div>`;
          if (!all.length) return `<div class="fs-live-arrivals-panel" id="fs-live-panel">${titleHtml}<div class="fs-live-empty">No arrival rows found yet</div></div>`;
          return `<div class="fs-live-arrivals-panel" id="fs-live-panel">${titleHtml}<div class="fs-live-list">${all.join('')}</div></div>`;
        }

        // ─────────────────────────────────────────────────────────────
        // LIVE ARRIVALS — bind refresh button
        // ─────────────────────────────────────────────────────────────
        function bindRefresh(panel) {
          if (!panel || panel.dataset.refreshBound) return;
          panel.dataset.refreshBound = '1';

          panel.addEventListener('click', async (e) => {
            const btn = e.target.closest('#fs-live-refresh');
            if (!btn || btn.dataset.spinning === '1') return;

            e.preventDefault();
            e.stopPropagation();

            btn.dataset.spinning = '1';
            window._fsRefreshing = true;

            btn.style.animation = 'none';
            btn.style.transform = 'rotate(0deg)';
            setTimeout(() => {
              btn.style.animation = 'fsRefreshSpin 0.8s linear infinite';
            }, 20);

            try {
              scrapeDirty = true;
              scrapeCache = null;

              acAt = 0;
              acCache = null;
              acIndex = {};
              acOk = false;

              await refreshAcCache();

              const html = await buildLivePanelHtml();
              if (!html) return;

              const tmp = document.createElement('div');
              tmp.innerHTML = html.trim();
              const rebuilt = tmp.firstElementChild;
              if (!rebuilt) return;

              const currentPanel = document.getElementById('fs-live-panel');
              if (!currentPanel) return;

              // update timestamp/header content
              const newUpdated = rebuilt.querySelector('.fs-live-updated');
              const curUpdated = currentPanel.querySelector('.fs-live-updated');
              if (newUpdated && curUpdated) {
                curUpdated.innerHTML = newUpdated.innerHTML;
              }

              // replace list only, keep same root panel alive
              const newList = rebuilt.querySelector('.fs-live-list, .fs-live-empty');
              const curList = currentPanel.querySelector('.fs-live-list, .fs-live-empty');
              if (newList && curList) {
                curList.replaceWith(newList);
              } else if (newList && !curList) {
                currentPanel.appendChild(newList);
              }

              if (window._fsAcStates?.length) {
                renderInbound(detectInbound(window._fsAcStates));
              }
            } catch (err) {
              console.log('fs-live-refresh error', err);
            } finally {
              window._fsRefreshing = false;

              setTimeout(() => {
                const live = document.getElementById('fs-live-panel');
                const stillBtn = live ? live.querySelector('#fs-live-refresh') : null;
                if (stillBtn) {
                  stillBtn.style.animation = 'none';
                  stillBtn.dataset.spinning = '';
                }
              }, 800);
            }
          });
        }
        // ─────────────────────────────────────────────────────────────
        // LIVE ARRIVALS — periodic in-place updater
        // ─────────────────────────────────────────────────────────────
        async function updateLive() {
          const panel = document.getElementById('fs-live-panel'); if (!panel) return;
          bindRefresh(panel);
          const cards = panel.querySelectorAll('.fs-live-card'); if (!cards.length) return;
          await refreshAcCache();
          const cfg = getSettings();
          for (const card of cards) {
            if (card.dataset.state === 'landed') continue;
            const cs  = card.dataset.cs||'';
            const airline = card.dataset.airline, flight = card.dataset.flight;
            const icon = card.dataset.icon, from = card.dataset.from;
            const sched= card.dataset.sched, actual = card.dataset.actual;
            const dg   = card.dataset.dg, ag = card.dataset.ag;
            const op   = card.dataset.op || operatorFromCallsign(cs) || operatingCarrier(airline,flight);
            let initD  = parseFloat(card.dataset.init)||0;

            function applyTracked(ac) {
              if (!ac) return false;
              const dist = haversine(ac.lat,ac.lon,JAC_LAT,JAC_LON);
              if (!initD || initD < dist) { initD = Math.round(dist); card.dataset.init = initD; }
              card.dataset.cs    = ac.rawCallsign;
              card.dataset.state = 'inbound';
              card.innerHTML = trackedCardInner(ac,icon,airline,flight,from,dg,ag,op,initD);
              return true;
            }

            // 1. Try direct callsign lookup (O(1))
            if (cs && applyTracked(lookupCallsign(cs))) continue;
            // 2. Try candidate list O(1) lookups
            const cands = candidates(airline,flight);
            let ac = null;
            for (const c of cands) { ac = lookupCallsign(c); if (ac) break; }
            // 3. Fuzzy fallback (scores through full cache)
            if (!ac) ac = fuzzyMatch(cands);
            if (ac) { applyTracked(ac); continue; }
            // 4. Fallback card
            const fb = fallbackCardInner(
              icon,
              airline,
              flight,
              from,
              sched,
              actual,
              dg,
              ag,
              op
            );
            card.dataset.state = fb.state; card.dataset.cs = ''; card.innerHTML = fb.html;
          }
          // Update timestamp
          const updEl = panel.querySelector('.fs-live-updated');
          if (updEl) {
            updEl.innerHTML = formatUpdatedLabel(true);
            clearTimeout(window._fsUpdatedFlashTimer);
            window._fsUpdatedFlashTimer = setTimeout(() => {
              const liveUpd = document.querySelector('.fs-live-updated');
              if (liveUpd) liveUpd.innerHTML = formatUpdatedLabel(false);
            }, 1000);
          }
          // Append newly landed
          const knownFlights = new Set([...cards].map(c => c.dataset.flight));
          let list = panel.querySelector('.fs-live-list');
          arrivalRows().forEach(f => {
            if (!(f.status||'').toLowerCase().includes('arrived') || !recentLanding(f)) return;
            if (knownFlights.has(f.flight)) return;
            const ma = minsAgo(f.actual||f.sched||'');
            const oc = airportCode(f.from)||f.from||'';
            const lbl= ma!=null&&ma<1?'Just landed':`Landed ${ma} min ago`;
            const nc = document.createElement('div');
            nc.className='fs-live-card fs-landed-card'; nc.dataset.state='landed'; nc.dataset.flight=f.flight;
            nc.innerHTML=`<div class="fs-landed-top">
              <span class="fs-landed-flight">${titleRow(liveIconHtml(f.airlineIcon),f.airline,f.flight)}</span>
              <span class="fs-landed-route">${oc?oc+' → JAC':'→ JAC'}</span>
            </div>
            <div class="fs-landed-bottom">
              <span class="fs-live-status">✓ Arrived</span>
              <span>${lbl}</span>
              ${f.actual?`<span>• ${f.actual}</span>`:''}
            </div>`;
            if (!list) { panel.querySelector('.fs-live-empty')?.remove(); list=document.createElement('div'); list.className='fs-live-list'; panel.appendChild(list); }
            list.appendChild(nc);
          });
        }
        if (window._fsLiveTimer) clearInterval(window._fsLiveTimer);
        window._fsLiveTimer = setInterval(() => {
          if (!window._fsRefreshing) updateLive();
        }, LIVE_INTERVAL);

        // ─────────────────────────────────────────────────────────────
        // SHEET MOUNT — creates overlay + sheet DOM, sets up drag
        // ─────────────────────────────────────────────────────────────
        function mountSheet(startDocked) {
          if (document.getElementById('fs-alerts-overlay')) return document.getElementById('fs-alerts-overlay');
          const cfg = getSettings();
          const m   = metrics();
          const startY = startDocked ? m.dock : m.hide;
          const dockEnabled = cfg.dock === true;

          const overlay = document.createElement('div');
          overlay.id = 'fs-alerts-overlay';
          overlay.style.cssText = 'position:fixed;inset:0;background:transparent;z-index:2147483646;pointer-events:none;';

          const sheet = document.createElement('div');
          sheet.id = 'fs-alerts-sheet';
          sheet.style.cssText = `
            position:absolute;left:50%;bottom:0;
            width:100%;max-width:920px;height:${m.h}px;
            transform:translate3d(-50%,${startY}px,0);
            background:var(--fs-panel-bg);
            border:1px solid var(--fs-panel-border);
            backdrop-filter:blur(14px) saturate(180%);
            -webkit-backdrop-filter:blur(14px) saturate(180%);
            box-shadow:var(--fs-shadow);
            border-radius:24px 24px 0 0;
            box-sizing:border-box;padding:0 8px;
            overflow:visible;will-change:transform;
            contain:layout paint style;
            backface-visibility:hidden;pointer-events:auto;`;

          const content = document.createElement('div');
          content.id = 'fs-alerts-content';
          content.style.cssText = 'height:100%;overflow-y:auto;-webkit-overflow-scrolling:touch;box-sizing:border-box;padding-bottom:calc(24px + env(safe-area-inset-bottom,0px) + 80px);';

          sheet.appendChild(content);
          overlay.appendChild(sheet);
          document.body.appendChild(overlay);

          // ── drag state
          let curY = startY, dragging = false, dragStartY = 0, dragStartOff = 0, listenersAttached = false;

          function setY(y) {
            curY = Math.max(0, Math.min(dockEnabled ? m.dock : m.h + 40, y));
            sheet.style.transform = `translate3d(-50%,${curY}px,0)`;
            const docked = dockEnabled && Math.abs(curY - m.dock) < 4;
            overlay.style.pointerEvents = docked ? 'none' : 'auto';
            sheetActionsVisibility(curY, m.dock, dockEnabled);
          }
          window.fsSetY      = setY;
          window.fsGetY      = () => curY;
          window.fsDockY     = () => m.dock;

          function onDragStart(y) {
            if (!dockEnabled) return;
            dragging=true; dragStartY=y; dragStartOff=curY;
            sheet.style.transition='none'; overlay.style.pointerEvents='auto';
          }
          function onDragMove(y) { if (!dockEnabled||!dragging) return; setY(dragStartOff+(y-dragStartY)); }
          function onDragEnd()   {
            if (!dockEnabled||!dragging) return;
            dragging=false;
            sheet.style.transition='transform .36s cubic-bezier(.22,1,.36,1)';
            setY(curY < m.dock*.5 ? 0 : m.dock);
            overlay.style.pointerEvents='auto';
          }
          function attachDrag() {
            if (listenersAttached) return; listenersAttached = true;
            window.addEventListener('mousemove', e => onDragMove(e.clientY));
            window.addEventListener('mouseup',   () => onDragEnd());
          }
          window.fsRefreshDock = enabled => {
            const h = sheet.querySelector('.fs-sheet-header');
            if (h) h.classList.toggle('fs-draggable', enabled);
            if (enabled) attachDrag();
          };

          // touch drag on header
          sheet.addEventListener('touchstart', e => {
            if (!e.target.closest('.fs-sheet-header,.fs-ops-summary,.fs-weather-banner,.fs-sheet-preview-tools')) return;
            onDragStart(e.touches[0].clientY);
          }, { passive:true });
          sheet.addEventListener('touchmove', e => {
            if (!dragging) return; e.preventDefault(); onDragMove(e.touches[0].clientY);
          }, { passive:false });
          sheet.addEventListener('touchend', () => onDragEnd());

          // backdrop tap → dismiss or dock
          overlay.addEventListener('click', e => {
            const sp = document.getElementById('fs-settings-panel');
            const sb = document.getElementById('fs-settings-btn');
            const sm = document.getElementById('fs-sheet-menu');
            if (sheet.contains(e.target)||(sb&&sb.contains(e.target))||(sp&&sp.contains(e.target))||(sm&&sm.contains(e.target))) return;
            dismissSettings();
            dismissSheetMenu();
            if (dockEnabled) { sheet.style.transition='transform .36s cubic-bezier(.22,1,.36,1)'; setY(m.dock); }
            else slideOut();
          });
          return overlay;
        }

        // ─────────────────────────────────────────────────────────────
        // SHEET VISIBILITY (dock fade/blur header when partially visible)
        // ─────────────────────────────────────────────────────────────
        function sheetActionsVisibility(curY, dockY, dockEnabled) {
          const sheet = document.getElementById('fs-alerts-sheet');
          const hdr = document.querySelector('.fs-sheet-header');
          const act = document.querySelector('.fs-sheet-actions');
          if (!hdr) return;
          if (!dockEnabled) {
            sheet?.classList.remove('fs-sheet-collapsed','fs-sheet-peeking');
            sheet?.classList.add('fs-sheet-expanded');
            document.documentElement.classList.remove('fs-alerts-collapsed');
            hdr.style.opacity='1'; hdr.style.transform='scale(1) translateY(0px)'; hdr.style.filter='blur(0)'; hdr.style.pointerEvents='auto';
            if (act) { act.style.opacity='1'; act.style.transform='translateY(0) scale(1)'; act.style.filter='blur(0)'; act.style.pointerEvents='auto'; }
            return;
          }
          const prog  = Math.max(0,Math.min(1,curY/Math.max(dockY,1)));
          sheet?.classList.toggle('fs-sheet-expanded', prog < 0.18);
          sheet?.classList.toggle('fs-sheet-peeking', prog >= 0.18 && prog < 0.72);
          sheet?.classList.toggle('fs-sheet-collapsed', prog >= 0.72);
          document.documentElement.classList.toggle('fs-alerts-collapsed', prog >= 0.72);
          const vis   = Math.pow(1-prog, 1.65);
          const visAct= Math.pow(1-prog, 1.9);
          hdr.style.opacity       = vis;
          hdr.style.transform     = `scale(${0.92+0.08*vis}) translateY(${(1-vis)*10}px)`;
          hdr.style.filter        = `blur(${(1-vis)*8}px)`;
          hdr.style.pointerEvents = vis<0.08?'none':'auto';
          if (act) {
            act.style.opacity       = visAct;
            act.style.transform     = `translateY(${(1-visAct)*12}px) scale(${0.88+0.12*visAct})`;
            act.style.filter        = `blur(${(1-visAct)*10}px)`;
            act.style.pointerEvents = visAct<0.08?'none':'auto';
          }
        }

        // ─────────────────────────────────────────────────────────────
        // SLIDE OUT HELPER
        // ─────────────────────────────────────────────────────────────
        function slideOut() {
          const overlay = document.getElementById('fs-alerts-overlay');
          const sheet   = document.getElementById('fs-alerts-sheet');
          if (!overlay||!sheet) return;
          const m = metrics();
          if (getSettings().dock === true) {
            sheet.style.transition='transform .34s cubic-bezier(.22,1,.36,1),opacity .18s ease';
            sheet.style.opacity='1';
            window.fsSetY?.(m.dock);
            return;
          }
          sheet.style.transition='transform .34s cubic-bezier(.22,1,.36,1),opacity .22s ease';
          sheet.style.opacity='0'; sheet.style.transform=`translate3d(-50%,${m.hide}px,0)`;
          setTimeout(() => overlay.parentNode?.removeChild(overlay), 340);
        }

        function dismissSheetMenu() {
          const p = document.getElementById('fs-sheet-menu');
          if (!p) return;
          p.style.transition='opacity .18s ease,transform .20s ease';
          p.style.opacity='0';
          p.style.transform='translateY(6px) scale(0.98)';
          setTimeout(() => p.parentNode?.removeChild(p), 185);
          document.removeEventListener('click', closeSheetMenuOutside);
        }

        function closeSheetMenuOutside(e) {
          const p = document.getElementById('fs-sheet-menu');
          const sheetBtn = document.getElementById('fs-sheet-menu-btn');
          const tabBtn = document.querySelector('.fs-tab[data-type="menu"]');
          if (!p) return;
          if (!p.contains(e.target) && (!sheetBtn||!sheetBtn.contains(e.target)) && (!tabBtn||!tabBtn.contains(e.target))) dismissSheetMenu();
        }

        function sheetPanelStyle(width, height, pad) {
          const sheet = document.getElementById('fs-alerts-sheet');
          const sr = sheet?.getBoundingClientRect?.();
          width = width || 268;
          height = height || 220;
          pad = pad || 14;
          if (!sr) {
            return `position:fixed;top:${pad}px;right:${pad}px;z-index:2147483647;`;
          }
          const right = Math.max(10, window.innerWidth - sr.right + pad);
          const top = Math.max(10, Math.min(window.innerHeight - height - 10, sr.top + pad));
          return `position:fixed;top:${top}px;right:${right}px;z-index:2147483647;`;
        }

        function toggleSheetMenu(anchor) {
          if (document.getElementById('fs-sheet-menu')) { dismissSheetMenu(); return; }
          const sheet = document.getElementById('fs-alerts-sheet');
          const p = document.createElement('div');
          p.id = 'fs-sheet-menu';
          if (sheet) {
            p.style.cssText = sheetPanelStyle(210, 210, 14);
          } else {
            const rect = anchor?.getBoundingClientRect?.() || { right: window.innerWidth - 18, top: window.innerHeight - 90 };
            const right = Math.max(10, window.innerWidth - rect.right);
            const bottom = Math.max(10, window.innerHeight - rect.top + 8);
            p.style.cssText = `position:fixed;bottom:${bottom}px;right:${right}px;z-index:2147483647;`;
          }
          const docked = getSettings().dock === true;
          const hasSheet = !!sheet;
          const expanded = hasSheet && (window.fsGetY?.() || 0) < 12;
          const primaryLabel = docked
            ? (expanded ? 'Dock sheet' : 'Open sheet')
            : (expanded ? 'Close sheet' : 'Open sheet');
          p.innerHTML = `
            <button class="fs-menu-item" data-action="settings">
              <span>Settings</span>
              <svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-13.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l119-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24 12L590-80H370Z"/></svg>
            </button>
            <button class="fs-menu-item" data-action="collapse">
              <span>${primaryLabel}</span>
              <svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm80-160h400v-80H280v80Zm0-160h400v-80H280v80Z"/></svg>
            </button>
            ${docked ? `<button class="fs-menu-item" data-action="undock"><span>Show bottom tabs</span><svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="M240-120v-80h480v80H240Zm-80-160v-560h640v560H160Zm80-80h480v-400H240v400Z"/></svg></button>` : ''}
          `;
          p.addEventListener('click', e => {
            const item = e.target.closest('.fs-menu-item'); if (!item) return;
            const action = item.dataset.action;
            if (action === 'settings') {
              dismissSheetMenu();
              openSheetThenSettings();
            } else if (action === 'collapse') {
              dismissSheetMenu();
              if (docked) {
                if (!document.getElementById('fs-alerts-overlay')) openAlertsSheet();
                setTimeout(() => {
                  const m = metrics();
                  window.fsRefreshDock?.(true);
                  window.fsSetY?.(expanded ? m.dock : 0);
                }, 90);
              } else {
                if (expanded) slideOut();
                else openAlertsSheet();
              }
            } else if (action === 'undock') {
              dismissSheetMenu();
              saveSettings({ ...getSettings(), dock: false });
              updateDockTabBar();
              document.documentElement.classList.remove('fs-alerts-collapsed');
              slideOut();
            }
          });
          document.body.appendChild(p);
          setTimeout(() => document.addEventListener('click', closeSheetMenuOutside), 10);
        }

        function openSheetThenSettings() {
          const sheet = document.getElementById('fs-alerts-sheet');
          const hasOverlay = !!document.getElementById('fs-alerts-overlay');
          const cur = window.fsGetY?.() ?? 0;
          if (!hasOverlay || !sheet) {
            openAlertsSheet();
            setTimeout(() => toggleSettings(), 430);
            return;
          }
          if (getSettings().dock === true && cur > 12) {
            sheet.style.transition='transform .34s cubic-bezier(.22,1,.36,1)';
            window.fsSetY?.(0);
            setTimeout(() => toggleSettings(), 360);
            return;
          }
          toggleSettings();
        }

        // ─────────────────────────────────────────────────────────────
        // BIND CONTENT BUTTONS (called after every innerHTML write)
        // ─────────────────────────────────────────────────────────────
        function bindContentButtons() {
          document.getElementById('fs-close-btn')?.addEventListener('click', e => { e.stopPropagation(); handleAlertsAction(); });
          document.getElementById('fs-settings-btn')?.addEventListener('click', e => { e.stopPropagation(); toggleSettings(); });
          document.getElementById('fs-sheet-menu-btn')?.addEventListener('click', e => { e.stopPropagation(); toggleSheetMenu(e.currentTarget); });
        }

        // ─────────────────────────────────────────────────────────────
        // HYDRATE CONTENT — scrape + HTML build + DOM patch
        // Does NOT touch the sheet position, so animation is never blocked
        // ─────────────────────────────────────────────────────────────
        function hydrateContent(opts) {
          opts = opts||{};
          const cfg = getSettings();
          const dockEnabled = cfg.dock === true;
          const content = document.getElementById('fs-alerts-content'); if (!content) return;
          const data = scrapeAll(); if (!data) return;
          const existingPanel = !opts.refreshLive ? content.querySelector('#fs-live-panel') : null;

          const livePlaceholder = opts.refreshLive && cfg.liveArrivals !== false
            ? `<div class="fs-live-arrivals-panel" id="fs-live-panel">
                <div class="fs-live-panel-title">Live arrival status</div>
                <div class="fs-live-empty">Loading…</div>
               </div>`
            : '';
          const scrollTop = content.scrollTop;
          content.innerHTML = buildContentHtml(data, livePlaceholder);
          if (existingPanel) {
            existingPanel.dataset.refreshBound = '';  // clear bound flag
            const anchor = content.querySelector('.fs-weather-banner') || content.querySelector('.fs-sheet-header-wrap');
            anchor?.insertAdjacentElement('afterend', existingPanel) || content.prepend(existingPanel);
            bindRefresh(existingPanel);
          }
          content.scrollTop = scrollTop;
          bindContentButtons();
          updateDockIcon();
          window.fsRefreshDock?.(dockEnabled);
          // Re-inject inbound panel from cached states (wiped by innerHTML above)
          if (window._fsAcStates?.length) {
            renderInbound(detectInbound(window._fsAcStates));
          } else {
            refreshAcCache();
          }
          // Load live arrivals in background
          if (opts.refreshLive && cfg.liveArrivals !== false) {
            buildLivePanelHtml().then(html => {
              const old = document.getElementById('fs-live-panel'); if (!old) return;
              if (html) {
              const tmp = document.createElement('div');
              tmp.innerHTML = html;
              const newPanel = tmp.firstElementChild;
              delete newPanel.dataset.refreshBound;
              old.replaceWith(newPanel);
              bindRefresh(newPanel);
              }
              else old.remove();
            }).catch(() => {});
          }
          // Re-apply current sheet offset
          if (typeof window.fsSetY === 'function') requestAnimationFrame(() => window.fsSetY(window.fsGetY?.()));
        }

        // ─────────────────────────────────────────────────────────────
        // OPEN ALERTS SHEET — skeleton first, animation clean, hydrate idle
        // ─────────────────────────────────────────────────────────────
        function openAlertsSheet() {
          const cfg = getSettings();
          // 1. Mount DOM (no content, instant)
          mountSheet(false);
          const sheet   = document.getElementById('fs-alerts-sheet');
          const overlay = document.getElementById('fs-alerts-overlay');
          const content = document.getElementById('fs-alerts-content');
          if (!sheet||!content) return;
          // 2. Show skeleton immediately
          content.innerHTML = `<div style="padding:24px 16px">
            <div class="fs-skeleton-line wide"></div>
            <div class="fs-skeleton-line med"></div>
            <div class="fs-skeleton-line short"></div>
            <div class="fs-skeleton-line wide" style="margin-top:20px"></div>
            <div class="fs-skeleton-line med"></div>
          </div>`;
          // 3. Start slide animation in next rAF — thread is free
          requestAnimationFrame(() => {
            sheet.style.transition = 'transform .36s cubic-bezier(.22,1,.36,1),opacity .18s ease';
            sheet.style.opacity = '1';
            overlay.style.pointerEvents = 'auto';
            window.fsSetY?.(0) || (sheet.style.transform = 'translate3d(-50%,0px,0)');
            // 4. Hydrate real content during idle time AFTER animation starts
            fsIdle(() => hydrateContent({ refreshLive: true }), 400);
          });
        }

        // ─────────────────────────────────────────────────────────────
        // RE-RENDER (sheet already mounted, triggered by MutationObserver)
        // ─────────────────────────────────────────────────────────────
        function renderAlerts(opts) {
          const overlay = document.getElementById('fs-alerts-overlay');
          if (!overlay) return;
          fsIdle(() => hydrateContent(opts||{ refreshLive: false }), 300);
        }

        // ─────────────────────────────────────────────────────────────
        // HANDLE ALERTS ACTION (close button / dock toggle)
        // ─────────────────────────────────────────────────────────────
        function handleAlertsAction() {
          const overlay = document.getElementById('fs-alerts-overlay');
          const sheet   = document.getElementById('fs-alerts-sheet');
          const cfg     = getSettings();
          const tabBar  = document.getElementById('fs-bottom-tabs');
          // Reset tab bar to alerts state
          if (tabBar) {
            tabBar.className = tabBar.className.replace(/\barrivals\b|\bdepartures\b/g,'').trim() + ' alerts';
            tabBar.querySelectorAll('.fs-tab').forEach(t => t.classList.remove('active'));
            tabBar.querySelector('.fs-tab[data-type="alerts"]')?.classList.add('active');
          }
          dismissSettings();
          if (!overlay||!sheet) { openAlertsSheet(); return; }
          const m   = metrics();
          const cur = window.fsGetY?.() ?? 0;
          sheet.style.transition = 'transform .36s cubic-bezier(.22,1,.36,1),opacity .22s ease';
          if (cfg.dock === true) {
            sheet.style.opacity='1';
            window.fsSetY?.(cur<=2 ? window.fsDockY?.() ?? m.dock : 0);
          } else {
            if (cur <= 2) slideOut();
            else openAlertsSheet();
          }
        }

        // ─────────────────────────────────────────────────────────────
        // DOCK / ICON HELPERS
        // ─────────────────────────────────────────────────────────────
        function updateDockIcon() {
          const btn = document.getElementById('fs-close-btn'); if (btn) btn.innerHTML = dockIconSvg();
        }
        function updateDockTabBar() {
          const tabBar = document.getElementById('fs-bottom-tabs'); if (!tabBar) return;
          const docked = getSettings().dock === true;
          tabBar.classList.toggle('fs-docked', docked);
          document.documentElement.classList.toggle('fs-tabs-docked', docked);
        }

        // ─────────────────────────────────────────────────────────────
        // SETTINGS PANEL
        // ─────────────────────────────────────────────────────────────
        function dismissSettings() {
        document.getElementById('fs-radar-submenu')?.remove();
          const p = document.getElementById('fs-settings-panel'); if (!p) return;
          p.style.transition='opacity .22s ease,transform .22s ease';
          p.style.opacity='0'; p.style.transform='translateY(4px) scale(0.98)';
          setTimeout(() => p.parentNode?.removeChild(p), 215);
          document.removeEventListener('click', closeSettingsOutside);
        }

        function toggleSettings() {
          if (document.getElementById('fs-settings-panel')) { dismissSettings(); return; }
          const sheet = document.getElementById('fs-alerts-sheet'); if (!sheet) return;
          const docked = getSettings().dock === true;
          const curY = window.fsGetY?.() || 0;
          if (docked && curY > 12 && typeof window.fsSetY === 'function') {
            sheet.style.transition='transform .34s cubic-bezier(.22,1,.36,1)';
            window.fsSetY(0);
            setTimeout(() => toggleSettings(), 360);
            return;
          }
          const p = document.createElement('div');
          p.id = 'fs-settings-panel';
          const panelW = 268;
          const panelH = 390;
          p.style.cssText = sheetPanelStyle(panelW, panelH, 14);
          document.body.appendChild(p);
          p.addEventListener('click', e => e.stopPropagation());
          renderSettingsPage('main');
          setTimeout(() => document.addEventListener('click', closeSettingsOutside), 10);
        }

        function settingsTitle(title, back) {
          return `<div class="fs-settings-heading ${back ? 'has-back' : ''}">
            ${back ? `<button class="fs-settings-back" data-page="${back}" aria-label="Back">
              <svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor"><path d="M560-240 320-480l240-240 56 56-184 184 184 184-56 56Z"/></svg>
            </button>` : ''}
            <span>${title}</span>
          </div>`;
        }

        function navRow(id, label, page) {
          return `<div class="fs-setting fs-setting-nav" id="${id}" data-page="${page}" style="cursor:pointer;">
            <span>${label}</span>
            <svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor" style="opacity:.5;flex-shrink:0;margin-left:auto;">
              <path d="M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z"/>
            </svg>
          </div>`;
        }

        function renderSettingsPage(page) {
          const p = document.getElementById('fs-settings-panel'); if (!p) return;
          const s = getSettings();
          p.dataset.page = page;
          if (page === 'radar') {
            p.innerHTML = `
              ${settingsTitle('Radar live traffic', 'main')}
              <label class="fs-setting"><span>Inbound aircraft panel</span><input type="checkbox" id="s-inbound"></label>
              <label class="fs-setting"><span>Approach confidence</span><input type="checkbox" id="s-conf"></label>
              <label class="fs-setting"><span>Color-coded alt/speed</span><input type="checkbox" id="s-colors"></label>
              ${navRow('s-range-nav', `Radar range <em>${parseInt(s.distance)||80} mi</em>`, 'range')}`;
          } else if (page === 'range') {
            p.innerHTML = `
              ${settingsTitle('Radar range', 'radar')}
              <div class="fs-setting-distance">
                <div class="fs-dist-presets">
                  ${[20,40,60,80,120,500,1500].map(d=>`<span data-d="${d}">${d}</span>`).join('')}
                </div>
                <div class="fs-dist-modes"><span>APP</span><span>TWR</span><span>TRM</span><span>REG</span><span>MAX</span><span>EXT</span><span>ULT</span></div>
                <input type="range" id="s-range" min="20" max="1500" step="1">
                <div class="fs-dist-value" id="s-dist-val"></div>
              </div>`;
          } else {
            p.innerHTML = `
              ${settingsTitle('Settings', '')}
              <label class="fs-setting"><span>Live status <em>Beta</em></span><input type="checkbox" id="s-live"></label>
              <label class="fs-setting"><span>Weather warnings</span><input type="checkbox" id="s-wx"></label>
              <label class="fs-setting"><span>Keep alerts docked</span><input type="checkbox" id="s-dock"></label>
              ${navRow('s-radar-nav', 'Radar & live traffic', 'radar')}`;
          }
          p.querySelectorAll('[data-page]').forEach(el => {
            el.addEventListener('click', e => {
              e.stopPropagation();
              renderSettingsPage(el.dataset.page || 'main');
            });
          });
          loadSettingsUI();
          attachSettingsHandlers();
        }

        function closeSettingsOutside(e) {
          const p = document.getElementById('fs-settings-panel');
          const b = document.getElementById('fs-settings-btn');
          const mb = document.getElementById('fs-sheet-menu-btn');
          const tb = document.querySelector('.fs-tab[data-type="menu"]');
          if (!p) return;
          if (!p.contains(e.target) && (!b||!b.contains(e.target)) && (!mb||!mb.contains(e.target)) && (!tb||!tb.contains(e.target))) dismissSettings();
        }

        function getDistMode(d) {
          d = parseInt(d)||80;
          return d<=20?'Approach':d<=40?'Tower':d<=60?'Terminal':d<=80?'Regional':d<=120?'Max':d<=500?'Extended':'Ultra';
        }

        function loadSettingsUI() {
          const s = getSettings();
          const dk = s.dock===true;
          const get = id => document.getElementById(id);
          const set = (id, v) => { const el=get(id); if(el) el.checked=v; };
          set('s-live',    s.liveArrivals!==false);
          set('s-dock',    dk);
          set('s-inbound', s.inbound!==false);
          set('s-conf',    s.confidence!==false);
          set('s-colors',  s.colors!==false);
          set('s-wx',      s.weather!==false);
          const wxEl = get('s-wx'); if (wxEl) { wxEl.disabled=false; wxEl.style.opacity='1'; }
          const d = parseInt(s.distance)||80;
          const range = get('s-range'); if (range) range.value = d;
          const val   = get('s-dist-val'); if (val) val.textContent = `${d} mi • ${getDistMode(d)}`;
          document.querySelectorAll('.fs-dist-presets span').forEach(sp => sp.classList.toggle('active', parseInt(sp.dataset.d)===d));
        }

        function attachSettingsHandlers() {
          const get = id => document.getElementById(id);
          const dockTgl = get('s-dock'), liveTgl = get('s-live'), wxTgl = get('s-wx');
          const inbTgl  = get('s-inbound'), confTgl = get('s-conf'), colorTgl = get('s-colors');
          const range   = get('s-range'), valEl = get('s-dist-val');

          function save() {
            const cur = getSettings();
            const dk = dockTgl ? dockTgl.checked : cur.dock===true;
            saveSettings({
              liveArrivals: liveTgl ? liveTgl.checked : cur.liveArrivals!==false,
              dock:         dk,
              weather:      wxTgl ? wxTgl.checked : cur.weather!==false,
              inbound:      inbTgl ? inbTgl.checked : cur.inbound!==false,
              confidence:   confTgl ? confTgl.checked : cur.confidence!==false,
              colors:       colorTgl ? colorTgl.checked : cur.colors!==false,
              distance:     range ? (parseInt(range.value)||80) : (parseInt(cur.distance)||80)
            });
          }

          function updateDist(d) {
            d=parseInt(d)||80;
            if(range) range.value=d;
            if(valEl) valEl.textContent=`${d} mi • ${getDistMode(d)}`;
            document.querySelectorAll('.fs-dist-presets span').forEach(sp => sp.classList.toggle('active', parseInt(sp.dataset.d)===d));
          }

          function applyLive(opts) {
            save();
            const s = getSettings();
            // Apply inbound panel visibility
            if (window._fsAcStates?.length) renderInbound(detectInbound(window._fsAcStates));
            else { const el=document.getElementById('fs-inbound-aircraft'); if(el) el.style.display=s.inbound===false?'none':''; }
            // Apply color/conf patches without full re-render
            document.querySelectorAll('.fs-live-card[data-state="inbound"]').forEach(card => {
              ['fs-live-alt','fs-live-spd'].forEach(cls => {
                const el = card.querySelector('.'+cls); if (!el) return;
                const isAlt = cls==='fs-live-alt';
                el.classList.remove('alt-low','alt-mid','alt-high','spd-slow','spd-approach','spd-fast');
                if (s.colors!==false) {
                  const v = parseInt(isAlt?el.dataset.alt:el.dataset.spd)||0;
                  el.classList.add(isAlt?altClass(v):spdClass(v));
                }
              });
              card.querySelectorAll('.fs-conf-row').forEach(r => r.style.display=s.confidence!==false?'':'none');
            });
            // Weather banner
            const wb = document.querySelector('.fs-weather-banner');
            if (wb) wb.style.display=s.weather!==false?'':'none';
            if (!opts?.skipLive) updateLive();
          }

          if (dockTgl) {
            dockTgl.addEventListener('change', () => {
              if (wxTgl) { wxTgl.disabled=false; wxTgl.style.opacity='1'; }
              save(); updateDockIcon(); updateDockTabBar();
              window.fsRefreshDock?.(dockTgl.checked);
              dismissSettings();
              const overlay=document.getElementById('fs-alerts-overlay'), sheet=document.getElementById('fs-alerts-sheet');
              if (!overlay||!sheet) return;
              const m = metrics(); sheet.style.height = m.h+'px';
              sheet.style.transition='transform .36s cubic-bezier(.22,1,.36,1),opacity .18s ease';
              if (dockTgl.checked) { sheet.style.opacity='1'; window.fsSetY?.(m.dock); }
              else slideOut();
            });
          }
          if (liveTgl) liveTgl.addEventListener('change', () => { save(); openAlertsSheet(); });
          [inbTgl,confTgl,colorTgl,wxTgl].forEach(el => el?.addEventListener('change', () => applyLive()));
          if (range) {
            range.addEventListener('input',  () => updateDist(range.value));
            range.addEventListener('change', () => {
              updateDist(range.value); save();
              acAt=0; refreshAcCache().then(() => {
                if (window._fsAcStates?.length) renderInbound(detectInbound(window._fsAcStates));
              });
            });
          }
          document.querySelectorAll('.fs-dist-presets span').forEach(sp => {
            sp.addEventListener('click', () => {
              updateDist(sp.dataset.d); save();
              acAt=0; refreshAcCache().then(() => {
                if (window._fsAcStates?.length) renderInbound(detectInbound(window._fsAcStates));
              });
            });
          });
          updateDist(parseInt(range?.value)||80);
        }

        // ─────────────────────────────────────────────────────────────
        // DOCK RESTORE ON PAGE LOAD
        // ─────────────────────────────────────────────────────────────
        function queueDockRestore(n) {
          n=n||0;
          if (!SHOW_TABS||!IS_MAIN) return;
          if (getSettings().dock!==true) return;
          const con=document.getElementById('flight-container');
          if (con?.querySelector('table.jha-flights')) { restoreDockedAlerts(); return; }
          if (n<60) requestAnimationFrame(()=>queueDockRestore(n+1));
        }
        function restoreDockedAlerts() {
          if (!SHOW_TABS||!IS_MAIN||document.getElementById('fs-alerts-overlay')) return;
          if (getSettings().dock!==true) return;
          const con=document.getElementById('flight-container');
          if (!con?.querySelector('table.jha-flights')) return;
          const m = metrics();
          mountSheet(true);
          const sheet   = document.getElementById('fs-alerts-sheet');
          const content = document.getElementById('fs-alerts-content');
          if (!sheet||!content) return;
          sheet.style.transition='none'; sheet.style.opacity='1';
          window.fsSetY?.(m.dock);
          requestAnimationFrame(() => {
            sheet.style.transition='transform .36s cubic-bezier(.22,1,.36,1)';
            fsIdle(() => hydrateContent({ refreshLive:true }), 600);
          });
        }

        // ─────────────────────────────────────────────────────────────
        // CLEAN PAGE CHROME
        // ─────────────────────────────────────────────────────────────
        function cleanTop() {
          ['header.site-header.header-mobile','.jac-navbar','section.page-hero.-noimage','.fixed-triggers']
            .forEach(s => document.querySelector(s)?.remove());
          document.body.style.marginTop='100px';
          const tab=document.getElementById('fs-bottom-tabs');
          document.body.style.paddingBottom = (SHOW_TABS&&tab)
            ? `calc(${tab.offsetHeight+24}px + env(safe-area-inset-bottom,0px))` : '0px';
        }

        // ─────────────────────────────────────────────────────────────
        // BOOT
        // ─────────────────────────────────────────────────────────────
        cleanTop();

        // Inject CSS
        let styleEl = document.getElementById('fs_custom_style');
        if (!styleEl) { styleEl=document.createElement('style'); styleEl.id='fs_custom_style'; document.head.appendChild(styleEl); }
        styleEl.innerHTML = `__FS_CSS__`;
        applyFlightRowStatusClasses();
        applyFlightDateCounts();
        applyEnhancedTableCells();
        bindFlightRowDetails();
        syncWeatherSnapshot();
        setTimeout(syncAiBriefSnapshot, 900);

        // Main observer (tab bar guard)
        window.fsMainObserver?.disconnect();
        window.fsMainObserver = new MutationObserver(() => {
          if (!document.getElementById('fs-bottom-tabs')&&SHOW_TABS&&!window.fsReloading) window.fsReloading=true;
        });
        window.fsMainObserver.observe(document.documentElement,{childList:true,subtree:true});

        // Flight data observer → invalidate scrape cache, debounce re-render
        let _alertTimer = null;
        window.fsFlightsObserver?.disconnect();
        window.fsFlightsObserver = new MutationObserver(() => {
          scrapeDirty = true;
          applyFlightRowStatusClasses();
          applyFlightDateCounts();
          applyEnhancedTableCells();
          bindFlightRowDetails();
          syncWeatherSnapshot();
          clearTimeout(window.fsAiBriefSnapshotTimer);
          window.fsAiBriefSnapshotTimer = setTimeout(syncAiBriefSnapshot, 900);
          if (!document.getElementById('fs-alerts-overlay')) return;
          clearTimeout(_alertTimer);
          const isDocked = typeof window.fsGetY==='function' && typeof window.fsDockY==='function'
            && Math.abs(window.fsGetY()-window.fsDockY()) < 6;
          _alertTimer = setTimeout(() => renderAlerts({ refreshLive:false }), isDocked?700:450);
        });
        const flightsCon = document.getElementById('flight-container');
        if (flightsCon) window.fsFlightsObserver.observe(flightsCon,{childList:true,subtree:true});

        // ─────────────────────────────────────────────────────────────
        // TAB BAR
        // ─────────────────────────────────────────────────────────────
        if (!SHOW_TABS) {
          document.getElementById('fs-bottom-tabs')?.remove();
          document.getElementById('fs-progressive-bottom-blur')?.remove();
          return;
        }
        if (document.getElementById('fs-bottom-tabs')) return;

        let progressiveBlur = document.getElementById('fs-progressive-bottom-blur');
        if (!progressiveBlur) {
          progressiveBlur = document.createElement('div');
          progressiveBlur.id = 'fs-progressive-bottom-blur';
          document.documentElement.appendChild(progressiveBlur);
        }

        const tabBar = document.createElement('div');
        tabBar.id = 'fs-bottom-tabs';

        if (IS_MAIN) {
          tabBar.className = 'arrivals';
          tabBar.innerHTML = `
            <div class="fs-tab arrivals active" data-type="arrivals">
              <span class="icon"><svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M120-120v-80h720v80H120Zm622-202L120-499v-291l96 27 48 139 138 39-35-343 115 34 128 369 172 49q25 8 41.5 29t16.5 48q0 35-28.5 61.5T742-322Z"/></svg></span>
              Arrivals
            </div>
            <div class="fs-tab departures" data-type="departures">
              <span class="icon"><svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M120-120v-80h720v80H120Zm70-200L40-570l96-26 112 94 140-37-207-276 116-31 299 251 170-46q32-9 60.5 7.5T864-585q9 32-7.5 60.5T808-487L190-320Z"/></svg></span>
              Departures
            </div>
            <div class="fs-tab alerts" data-type="alerts">
              <span class="icon"><svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M40-120l440-760 440 760H40Zm138-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm-40-120h80v-200h-80v200Zm40-100Z"/></svg></span>
              Alerts
            </div>
            <div class="fs-tab menu" data-type="menu">
              <span class="icon"><svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z"/></svg></span>
              Menu
            </div>`;
        } else {
          tabBar.className = 'subpage';
          tabBar.innerHTML = `
            <div class="fs-tab sub-back active" data-type="sub-back">
              <span class="icon"><svg xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M640-80 215-480l400-400 71 71-329 329 329 329-71 71Z"/></svg></span>
              Back
            </div>
            <div class="fs-tab sub-toggle" data-type="sub-toggle">
              <span class="icon"><svg class="arrow" xmlns="http://www.w3.org/2000/svg" height="22" viewBox="0 -960 960 960" width="22" fill="currentColor"><path d="M480-528 296-344l-56-56 215-215 215 215-56 56-184-184Z"/></svg></span>
              <span class="toggle-label">Top</span>
            </div>`;
        }

        document.documentElement.appendChild(tabBar);
        updateDockTabBar();
        queueDockRestore();

        // Blur tabBar on scroll
        window.addEventListener('scroll', () => {
          const max = document.body.scrollHeight - window.innerHeight; if (max<=0) return;
          tabBar.style.backdropFilter = `blur(${18+(window.scrollY/max)*16}px) saturate(160%)`;
        });

        // Sub-page toggle state
        const toggleBtn = tabBar.querySelector('[data-type="sub-toggle"]');
        if (toggleBtn) {
          const arrow = toggleBtn.querySelector('.arrow'), lbl = toggleBtn.querySelector('.toggle-label');
          const updateToggle = () => {
            const atBottom = window.scrollY > (document.body.scrollHeight-window.innerHeight)/2;
            arrow?.classList.toggle('rotate',!atBottom);
            if (lbl) lbl.textContent = atBottom?'Top':'Bottom';
          };
          updateToggle();
          window.addEventListener('scroll', updateToggle);
        }

        // Tab click handler
        tabBar.addEventListener('click', e => {
          const tab = e.target.closest('.fs-tab'); if (!tab) return;
          const type = tab.dataset.type;

          if (type==='sub-back')   { window.location.href='https://www.jacksonholeairport.com/flights/'; return; }
          if (type==='sub-toggle') {
            const cur=window.scrollY, max=document.body.scrollHeight-window.innerHeight;
            window.scrollTo({ top:cur>max/2?0:document.body.scrollHeight, behavior:'smooth' }); return;
          }
          if (type==='menu') {
            tabBar.querySelectorAll('.fs-tab').forEach(t=>t.classList.remove('active'));
            tab.classList.add('active');
            toggleSheetMenu(tab);
            return;
          }

          tabBar.querySelectorAll('.fs-tab').forEach(t=>t.classList.remove('active'));
          tab.classList.add('active');
          const cfg = getSettings();
          const overlay = document.getElementById('fs-alerts-overlay');
          const sheet   = document.getElementById('fs-alerts-sheet');

          if (type==='alerts') {
            tabBar.className = tabBar.className.replace(/\barrivals\b|\bdepartures\b/g,'').trim()+' alerts';
            dismissSettings();
            if (!overlay||!sheet) {
              // ── FAST PATH — no sheet exists yet
              openAlertsSheet();
              return;
            }
            // ── Sheet already mounted — just move it (no work needed)
            const m   = metrics();
            const cur = window.fsGetY?.() ?? 0;
            sheet.style.transition='transform .36s cubic-bezier(.22,1,.36,1),opacity .22s ease';
            if (cfg.dock===true) {
              sheet.style.opacity='1';
              window.fsSetY?.(cur<=2 ? (window.fsDockY?.()??m.dock) : 0);
            } else {
              if (cur<=2) slideOut();
              else        openAlertsSheet();
            }
            return;
          }

          if (type==='arrivals') {
            tabBar.className=tabBar.className.replace(/\bdepartures\b|\balerts\b/g,'').trim()+' arrivals';
            if (cfg.dock!==true) overlay?.remove();
            document.querySelector('li[data-target="hide-departures"]')?.click();
          } else if (type==='departures') {
            tabBar.className=tabBar.className.replace(/\barrivals\b|\balerts\b/g,'').trim()+' departures';
            if (cfg.dock!==true) overlay?.remove();
            document.querySelector('li[data-target="hide-arrivals"]')?.click();
          }
          cleanTop();
        });

        })();
        """.trimIndent()
            .replace("__FS_CSS__", css)

        view?.evaluateJavascript(js, null)
    }
}
