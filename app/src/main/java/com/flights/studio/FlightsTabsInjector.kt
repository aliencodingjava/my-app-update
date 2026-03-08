package com.flights.studio

import android.webkit.WebView

object FlightsTabsInjector {

    fun injectHideTriggers(
        view: WebView?,
        showFlightsTabs: Boolean,
        isFlightsMain: Boolean
    ) {
        view?.evaluateJavascript(
            $$"""
        (function() {
        
        var SHOW_FLIGHT_TABS = $${if (showFlightsTabs) "true" else "false"};
        var IS_FLIGHTS_MAIN = $${if (isFlightsMain) "true" else "false"};
          
          // ===============================
          // AIRPORT IATA CODES
          // ===============================
          
          var airportCodes = {
            "Salt Lake City": "SLC",
            "Denver": "DEN",
            "Newark": "EWR",
            "Chicago": "ORD",
            "Dallas/Fort Worth": "DFW",
            "Los Angeles": "LAX",
            "San Francisco": "SFO",
            "Atlanta": "ATL"
          };
          
          var airlineICAO = {
            "United": "UAL",
            "Delta": "DAL",
            "American": "AAL",
            "Southwest": "SWA",
            "Alaska": "ASA",
            "JetBlue": "JBU",
            "Spirit": "NKS",
            "Frontier": "FFT"
          };
          
          //
          // WEATHER CACHE
          //
          
          var weatherContextCache = null;
          var weatherLastFetch = 0;
          
          function getTempColor(tempF){

            const t = parseInt(tempF);
            if (isNaN(t)) return "";

            if (t <= 10) return "#4aa3ff";     // arctic
            if (t <= 25) return "#2bbcff";     // very cold
            if (t <= 40) return "#38d4ff";     // freezing zone
            if (t <= 55) return "#39e6b0";     // cool
            if (t <= 70) return "#52e652";     // mild
            if (t <= 80) return "#ffe066";     // warm
            if (t <= 90) return "#ffb347";     // hot
            if (t <= 100) return "#ff7a3c";    // very hot
            return "#ff3d3d";                  // extreme heat
          }

          function getWeatherContextCached() {

            var now = Date.now();

            // refresh every 5 seconds
            if (!weatherContextCache || (now - weatherLastFetch) > 5000) {

              weatherContextCache = getWeatherContext();
              weatherLastFetch = now;

            }

            return weatherContextCache;
          }
          
          function generateSmartReason(delayMinutes, airline, flight, weather) {
     
          // 🌬 Strong winds
          if (delayMinutes >= 20 && weather && weather.windSpeed !== null && weather.windSpeed >= 20) {
            return "Strong crosswinds affecting operations";
          }
          
    

              var seed = (airline + flight).length;

              if (delayMinutes >= 180) {
                  return "Operational aircraft rotation disruption";
              }

              if (delayMinutes >= 120) {
                  var reasons = [
                      "Late inbound aircraft",
                      "Network traffic flow management",
                      "Aircraft repositioning delay"
                  ];
                  return reasons[seed % reasons.length];
              }

              if (delayMinutes >= 60) {
                  var reasons = [
                      "Air traffic congestion",
                      "Crew scheduling adjustment",
                      "Gate availability delay"
                  ];
                  return reasons[seed % reasons.length];
              }

              if (delayMinutes >= 30) {
                  return "Minor operational delay";
              }

              return "Schedule adjustment";
          }
          
          function calculateDelayMinutes(sched, actual) {

            function parseTime(str) {
                if (!str) return 0;

                var time = str.toLowerCase().trim();
                var isPM = time.includes("pm");

                time = time.replace("am","").replace("pm","").trim();

                var parts = time.split(":");
                var hours = parseInt(parts[0]);
                var minutes = parseInt(parts[1]);

                if (isPM && hours !== 12) hours += 12;
                if (!isPM && hours === 12) hours = 0;

                return hours * 60 + minutes;
            }

            var schedMin = parseTime(sched);
            var actualMin = parseTime(actual);

            var diff = actualMin - schedMin;

            if (diff < 0) diff += 24 * 60; // handle midnight edge case

            return diff;
          }
          
          function formatDelay(minutes) {

              if (!minutes || minutes <= 0) return "";

              var hours = Math.floor(minutes / 60);
              var mins = minutes % 60;

              if (hours > 0 && mins > 0) {
                  return hours + "h" + mins + "min";
              }

              if (hours > 0) {
                  return hours + "h";
              }

              return mins + "min";
          }
          
          function getWeatherWarnings(weather) {

            var warnings = [];

            // 👁 Low visibility
            if (weather.visibility !== null) {
                 var vis = weather.visibility;

                 if (!isNaN(vis) && vis <= 3) {
                warnings.push(`
                  <span class="fs-weather-warning vis-warning">
                    <svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor">
                      <path d="M792-56 624-222q-35 11-70.5 16.5T480-200q-151 0-269-83.5T40-500q21-53 53-98.5t73-81.5L56-792l56-56 736 736-56 56ZM480-320q11 0 20.5-1t20.5-4L305-541q-3 11-4 20.5t-1 20.5q0 75 52.5 127.5T480-320Zm292 18L645-428q7-17 11-34.5t4-37.5q0-75-52.5-127.5T480-680q-20 0-37.5 4T408-664L306-766q41-17 84-25.5t90-8.5q151 0 269 83.5T920-500q-23 59-60.5 109.5T772-302ZM587-486 467-606q28-5 51.5 4.5T559-574q17 18 24.5 41.5T587-486Z"/>
                      </svg>
                    Low Visibility
                  </span>
                `);
              }
            }
            
            // 🌬 Crosswind risk
            if (weather.windSpeed && weather.windSpeed >= 20) {
              warnings.push(`
                <span class="fs-weather-warning wind-warning">
                  <svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor">
                    <path d="M460-160q-50 0-85-35t-35-85h80q0 17 11.5 28.5T460-240q17 0 28.5-11.5T500-280q0-17-11.5-28.5T460-320H80v-80h380q50 0 85 35t35 85q0 50-35 85t-85 35ZM80-560v-80h540q26 0 43-17t17-43q0-26-17-43t-43-17q-26 0-43 17t-17 43h-80q0-59 40.5-99.5T620-840q59 0 99.5 40.5T760-700q0 59-40.5 99.5T620-560H80Zm660 320v-80q26 0 43-17t17-43q0-26-17-43t-43-17H80v-80h660q59 0 99.5 40.5T880-380q0 59-40.5 99.5T740-240Z"/>
                  </svg>
                  Crosswind
                </span>
              `);
            }

            // ❄ Icing risk
            if (weather.tempF) {
              var tempValue = parseInt(weather.tempF);

              if (!isNaN(tempValue) && tempValue <= 36) {
                warnings.push(`
                  <span class="fs-weather-warning ice-warning">
                    <svg xmlns="http://www.w3.org/2000/svg" height="16" viewBox="0 -960 960 960" width="16" fill="currentColor">
                      <path d="M800-560q-17 0-28.5-11.5T760-600q0-17 11.5-28.5T800-640q17 0 28.5 11.5T840-600q0 17-11.5 28.5T800-560ZM400-80v-144L296-120l-56-56 160-160v-64h-64L176-240l-56-56 104-104H80v-80h144L120-584l56-56 160 160h64v-64L240-704l56-56 104 104v-144h80v144l104-104 56 56-160 160v64h320v80H656l104 104-56 56-160-160h-64v64l160 160-56 56-104-104v144h-80Zm360-600v-200h80v200h-80Z"/>
                       </svg>
                    Icing Risk
                  </span>
                `);
              }
            }

            return warnings.join(" • ");
          }
          
          function getWeatherContext() {

            var tempF = document.querySelector(".cur-fahren");
            var tempC = document.querySelector(".cur-celcius");
            var iconWrap = document.querySelector(".icon-wrap, .cur-icon, .weather-icon");

            var fahrenheit = tempF ? tempF.textContent.trim() : "";
            var celsius = tempC ? tempC.textContent.trim() : "";

            // remove trailing slash if present
            fahrenheit = fahrenheit.replace("/", "").trim();

            var icon = "";
            if (iconWrap) {
              var svg = iconWrap.querySelector("svg");
              if (svg) icon = svg.outerHTML;
            }

            var windSpeed = null;
            var windDir = null;
            var visibilityValue = null;
            var cloudCoverage = null;

            var details = document.querySelector(".cur-details");

            if (details) {

              var text = details.innerText
                .replace(/\u00A0/g," ")
                .replace(/\s+/g," ")
                .trim();

              // VISIBILITY
              var vis = text.match(/Visibility:\s*([\d.]+)/i);
              if (vis) visibilityValue = parseFloat(vis[1]);

              // WIND
              var wind = text.match(/Wind:\s*(\d+)\s*mph\s*([A-Z]+)/i);
              if (wind) {
                windSpeed = parseInt(wind[1]);
                windDir = wind[2];
              }

              // CLOUD COVERAGE
              var cloud = text.match(/Cloud Coverage:\s*(\d+)/i);
              if (cloud) cloudCoverage = parseInt(cloud[1]);

            }

            return {
              tempF: fahrenheit,
              tempC: celsius,

              temp: (fahrenheit && celsius)
                ? fahrenheit + " / " + celsius
                : fahrenheit || celsius,

              windSpeed: windSpeed,
              windDir: windDir,

              visibility: visibilityValue,
              cloud: cloudCoverage,

              icon: icon
            };
          }
          
          function getWeatherClass(weather) {

            if (!weather || !weather.tempF) return "";

            var tempValue = parseInt(weather.tempF);
            if (isNaN(tempValue)) return "";

            if (tempValue <= 32) return "weather-freezing";
            if (tempValue <= 45) return "weather-cool";
            if (tempValue <= 70) return "weather-mild";
            if (tempValue <= 90) return "weather-warm";
            return "weather-hot";
          }


          /* ========================================
             AIRCRAFT TRACKING
          ======================================== */
          var aircraftCache = null;
          var aircraftLastFetch = 0;
          async function getAircraftInfo(callsign){

            try {

              const now = Date.now();

              // cache radar snapshot for 10 seconds
              if(!aircraftCache || (now - aircraftLastFetch) > 10000){

                const res = await fetch(
                  "https://opensky-network.org/api/states/all?lamin=40&lomin=-115&lamax=47&lomax=-104"
                );

                const data = await res.json();

                if(!data || !data.states) return null;

                aircraftCache = data.states;
                aircraftLastFetch = now;
              }

              for(let s of aircraftCache){

                if(!s[1]) continue;

                let cs = s[1].trim();

                let cleanCS = cs.replace(/\s+/g,"");
                let cleanCall = callsign.replace(/\s+/g,"");

                if(!cleanCS.includes(cleanCall)) continue;

                var lat = s[6];
                var lon = s[5];

                if(!lat || !lon) continue;

                var altitudeMeters = s[13] || s[7] || 0;
                var speedMS = s[9] || 0;

                var altitude = Math.round(altitudeMeters * 3.28084);
                var speed = Math.round(speedMS * 1.94384);

                return {
                  altitude: altitude,
                  speed: speed,
                  lat: lat,
                  lon: lon
                };

              }

            } catch(e){
              console.log("Aircraft API error", e);
            }

            return null;
          }


          function distanceMiles(lat1, lon1, lat2, lon2){

            var R = 3958.8;

            var dLat = (lat2-lat1) * Math.PI/180;
            var dLon = (lon2-lon1) * Math.PI/180;

            var a =
              Math.sin(dLat/2)*Math.sin(dLat/2) +
              Math.cos(lat1*Math.PI/180) *
              Math.cos(lat2*Math.PI/180) *
              Math.sin(dLon/2)*Math.sin(dLon/2);

            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

            return R * c;
          }
          
          function trackAircraftForAlerts(){

            if(!document.getElementById("fs-alerts-overlay")) return;

            var reasons = document.querySelectorAll(".fs-delay-reason");

            reasons.forEach(function(el){

              if(el.querySelector(".fs-aircraft-info")) return;

              var airline = el.dataset.airline;
              var flight = el.dataset.flight;

              if(!airline || !flight) return;

              var code = "";

              Object.keys(airlineICAO).forEach(function(key){
                if(airline.toLowerCase().indexOf(key.toLowerCase()) !== -1){
                  code = airlineICAO[key];
                }
              });

              var cleanFlight = flight.replace(/\s+/g,"");
              var callsign = (code + cleanFlight).toUpperCase();

              if(!callsign) return;

              getAircraftInfo(callsign).then(function(ac){

                if(!ac) return;

                var dist = distanceMiles(
                  ac.lat,
                  ac.lon,
                  43.6073,
                  -110.7377
                );

                if(dist > 800) return;
                var maxDistance = 500;

                var progress = Math.max(0, Math.min(1, (maxDistance - dist) / maxDistance));
                var percent = Math.round(progress * 100);

                var aircraftInfo = `
                  <div class="fs-aircraft-info">
                    ✈ ${callsign}
                    • ${ac.altitude} ft
                    • ${ac.speed} kt
                    • ${Math.round(dist)} mi away
                  </div>

                  <div class="fs-flight-progress">
                    <div class="fs-flight-progress-bar" style="width:${percent}%"></div>
                  </div>
                `;

                el.insertAdjacentHTML("beforeend", aircraftInfo);

              }).catch(function(e){
                console.log("Aircraft lookup failed", e);
              });

            });

          }

          
          function renderAlerts() {

            var container = document.getElementById("flight-container");
            if (!container) return;
            var weather = getWeatherContextCached();

            var tables = container.querySelectorAll("table.jha-flights");
            var lastUpdateEl = document.querySelector(".flight-table__time");
            var lastUpdate = lastUpdateEl ? lastUpdateEl.textContent.trim() : "";
            
            var arrivalsAlerts = [];
            var departuresAlerts = [];

            var totalDelayed = 0;
            var totalDiverted = 0;
            var totalCancelled = 0;
            var totalArrivalsToday = 0;
            var totalDeparturesToday = 0;

            // Unused
            var totalArrivalsTomorrow = 0;
            var totalDeparturesTomorrow = 0;
            var todayLabel = "";
            var tomorrowLabel = "";
            
            tables.forEach(function(table) {
            
            var wrapper = table.parentElement.parentElement;

            var isArrival = wrapper.classList.contains("-arrival");
            var isDeparture = wrapper.classList.contains("-departure");

            var rows = table.querySelectorAll("tbody tr");
            var currentDate = "";
            
            rows.forEach(function(row) {

              // 🗓 Date row
              var dayCell = row.querySelector(".day");

              if (dayCell) {
                currentDate = dayCell.textContent.trim();
                return;
              }

              // cache frequently used cells (important for performance)
              var statusSpan = row.querySelector(".status span");
              if (!statusSpan) return;

              var airlineCell = row.querySelector(".airline");
              var flightCell = row.querySelector(".flight");
              var schedCell = row.querySelector(".sched");
              var actualCell = row.querySelector(".actual");
              var fromCell = row.querySelector(".from");

              var statusLower = statusSpan.textContent.trim().toLowerCase();

              // Airline
              var airline = airlineCell ? airlineCell.innerText.trim() : "";

              var airlineIcon = "";
              if (airlineCell) {
                var img = airlineCell.querySelector("img");
                if (img) airlineIcon = img.outerHTML;
              }

              // Flight info (cached cells)
              var flight = flightCell ? flightCell.innerText : "";
              var sched = schedCell ? schedCell.innerText : "";
              var actual = actualCell ? actualCell.innerText : "";
              var from = fromCell ? fromCell.innerText : "";

              var airportCode = airportCodes[from] || "";

              // Status detection
              var isDelay = statusLower.includes("delay");
              var isDiverted = statusLower.includes("divert");
              var isCancelled = statusLower.includes("cancel");

              if (!isDelay && !isDiverted && !isCancelled) return;

              // Counters
              if (isCancelled) {
                totalCancelled++;
              }
              else if (isDiverted) {
                totalDiverted++;
              }
              else if (isDelay) {
                totalDelayed++;
              }

              if (isArrival) totalArrivalsToday++;
              if (isDeparture) totalDeparturesToday++;

              // Delay calculation
              var delayMinutes = isCancelled ? 0 : calculateDelayMinutes(sched, actual);

              // Reason
              
              var reason = isCancelled
                ? "Flight cancelled by airline"
                : isDiverted
                  ? "Flight diverted to alternate airport"
                  : generateSmartReason(delayMinutes, airline, flight, weather);


              var dateLabel = currentDate;

              var message = "<div class='fs-alert-date'>" + dateLabel + "</div>";

              message += `
                <div class="fs-flight-row">
                  ${airlineIcon ? "<span class='fs-airline-icon'>" + airlineIcon + "</span>" : ""}
                  <span class="fs-flight-name">${airline} ${flight}</span>
                  ${airportCode ? "<span class='fs-airport-code'>" + airportCode + "</span>" : ""}
              `;

              // CANCELLED
              if (isCancelled) {

                message += `
                  <span class='status-word cancelled-word'>CANCELLED</span>
                </div>

                <div class="fs-alert-times">
                  <span class="sched-time">Sched: ${sched}</span>
                </div>
                `;

              }

              // DIVERTED
              else if (isDiverted) {

                message += `
                  <span class='status-word diverted-word'>
                    <svg xmlns="http://www.w3.org/2000/svg"
                         height="14"
                         viewBox="0 -960 960 960"
                         width="14"
                         fill="currentColor">
                      <path d="M320-120q-66 0-113-47t-47-113q0-66 47-113t113-47h200q33 0 56.5-23.5T600-520q0-33-23.5-56.5T520-600H280v80L160-640l120-120v80h240q66 0 113 47t47 113q0 66-47 113t-113 47H320q-33 0-56.5 23.5T240-280q0 33 23.5 56.5T320-200h360v-80l120 120-120 120v-80H320Z"/>
                    </svg>
                    DIVERTED
                  </span>
                </div>
                `;

              }

              // DELAYED
              else if (delayMinutes > 0) {

                var plusFormat = "+" + delayMinutes + " min";

                message += `
                  <span class='status-word delayed-word'>DELAYED</span>
                  <span class='fs-delay-time'>${formatDelay(delayMinutes)}</span>
                  <span class='fs-plus-format'>(${plusFormat})</span>
                </div>

                <div class="fs-alert-times">
                  <span class="sched-time">Sched: ${sched}</span>
                  <span class="dot"> • </span>
                  <span class="actual-time">Actual: ${actual}</span>
                </div>
                `;

              }

              message += "<div class='fs-delay-reason' data-airline='" + airline + "' data-flight='" + flight + "'>" + reason + "</div>";

              if (isArrival) {
                arrivalsAlerts.push({
                  html: message,
                  delay: delayMinutes,
                  diverted: isDiverted,
                  cancelled: isCancelled
                });
              }

              if (isDeparture) {
                departuresAlerts.push({
                  html: message,
                  delay: delayMinutes,
                  diverted: isDiverted,
                  cancelled: isCancelled
                });
              }

            });

            });
            
            // Sorting
            
            arrivalsAlerts.sort(function(a, b) {

              if (a.cancelled && !b.cancelled) return -1;
              if (!a.cancelled && b.cancelled) return 1;

              if (a.diverted && !b.diverted) return -1;
              if (!a.diverted && b.diverted) return 1;

              return b.delay - a.delay;

            });
            
            departuresAlerts.sort(function(a, b) {

              if (a.cancelled && !b.cancelled) return -1;
              if (!a.cancelled && b.cancelled) return 1;

              if (a.diverted && !b.diverted) return -1;
              if (!a.diverted && b.diverted) return 1;

              return b.delay - a.delay;

            });
            
            showAlertsUI(
              arrivalsAlerts,
              departuresAlerts,
              totalDelayed,
              totalDiverted,
              totalCancelled,
              totalArrivalsToday,
              totalDeparturesToday,
              totalArrivalsTomorrow,
              totalDeparturesTomorrow,
              todayLabel,
              tomorrowLabel,
              lastUpdate,
              weather
            );
          }
          
          
          
          
          function cleanTop() {

              var header = document.querySelector('header.site-header.header-mobile');
              if (header) header.remove();

              var nav = document.querySelector('.jac-navbar');
              if (nav) nav.remove();

              var hero = document.querySelector('section.page-hero.-noimage');
              if (hero) hero.remove();

              var triggers = document.querySelectorAll('.fixed-triggers');
              triggers.forEach(function(el) { el.remove(); });

              document.body.style.marginTop = "100px";
              document.body.style.paddingTop = "0px";

              var tab = document.getElementById("fs-bottom-tabs");

              if (SHOW_FLIGHT_TABS && tab) {
                  var height = tab.offsetHeight;
                  document.body.style.paddingBottom =
                      "calc(" + (height + 24) + "px + env(safe-area-inset-bottom, 0px))";
              } else {
                  document.body.style.paddingBottom = "0px";
              }
          }
          
          function showAlertsUI(
            arrivalsAlerts,
            departuresAlerts,
            totalDelayed,
            totalDiverted,
            totalCancelled,
            totalArrivalsToday,
            totalDeparturesToday,
            totalArrivalsTomorrow,
            totalDeparturesTomorrow,
            todayLabel,
            tomorrowLabel,
            lastUpdate,
            weather
          ){

            // 🌦 WEATHER REACTIVE STYLING

            var isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;

            // =========================
            // OVERLAY (transparent click layer)
            // =========================
            var overlay = document.createElement("div");
            overlay.id = "fs-alerts-overlay";

            overlay.style.position = "fixed";
            overlay.style.left = "0";
            overlay.style.top = "0";
            overlay.style.width = "100%";
            overlay.style.height = "100%";
            overlay.style.background = "transparent";
            overlay.style.zIndex = "2147483646";
            overlay.style.display = "block";

            // =========================
            // SHEET (only glass surface)
            // =========================
            var sheet = document.createElement("div");
            sheet.id = "fs-alerts-sheet";

            sheet.style.position = "absolute";
            sheet.style.left = "50%";
            sheet.style.bottom = "0";
            sheet.style.transform = "translate3d(-50%, 100%, 0)";

            sheet.style.width = "100%";
            sheet.style.maxWidth = "920px";

            // =========================
            // DYNAMIC HEIGHT BASED ON TABS
            // =========================
            var tabBar = document.getElementById("fs-bottom-tabs");
            var bottomOffset = 0;

            if (tabBar) {
              var rect = tabBar.getBoundingClientRect();
              bottomOffset = window.innerHeight - rect.top;
            }

            var availableHeight = window.innerHeight - bottomOffset;
            sheet.style.height = (availableHeight * 0.8) + "px";

            // Glass material
            if (isDark) {
              sheet.style.background =
                "linear-gradient(135deg, rgba(12,18,30,0.90), rgba(6,10,18,0.88))";
            } else {
              sheet.style.background =
                "linear-gradient(135deg, rgba(255,255,255,0.65), rgba(240,245,255,0.45))";
            }

            sheet.style.backdropFilter = "blur(4px) saturate(140%)";
            sheet.style.webkitBackdropFilter = "blur(4px) saturate(140%)";
            sheet.style.boxShadow ="inset 0 10px 20px rgba(0,0,0,0.18)";

            sheet.style.borderTopLeftRadius = "24px";
            sheet.style.borderTopRightRadius = "24px";

            sheet.style.boxSizing = "border-box";
            sheet.style.padding = "12px";

            sheet.style.overflow = "visible";
            sheet.style.transition = "transform 0.36s cubic-bezier(.22,1,.36,1)";
            sheet.style.willChange = "transform";
            sheet.style.contain = "layout paint style";
            sheet.style.transformStyle = "preserve-3d";
            sheet.style.backfaceVisibility = "hidden";

            // =========================
            // INNER SCROLL CONTAINER
            // =========================
            var content = document.createElement("div");
            content.id = "fs-alerts-content";

            content.style.height = "100%";
            content.style.overflowY = "auto";
            content.style.webkitOverflowScrolling = "touch";
            content.style.boxSizing = "border-box";

            content.style.paddingBottom =
              "calc(24px + env(safe-area-inset-bottom, 0px) + 80px)";

            // =========================
            // Build content HTML
            // =========================

            var htmlParts = [];

            if (totalDelayed > 0 || totalDiverted > 0 || totalCancelled > 0) {

              htmlParts.push(`
                <div class="fs-summary">

                  <span class="fs-summary-icon">
                    <svg xmlns="http://www.w3.org/2000/svg"
                         height="16"
                         viewBox="0 -960 960 960"
                         width="16"
                         fill="currentColor">
                      <path d="M505.5-298.29q10.5-10.29 10.5-25.5t-10.29-25.71q-10.29-10.5-25.5-10.5t-25.71 10.29q-10.5 10.29-10.5 25.5t10.29 25.71q10.29 10.5 25.5 10.5t25.71-10.29ZM444-432h72v-240h-72v240ZM216-144q-29.7 0-50.85-21.15Q144-186.3 144-216v-528q0-29.7 21.15-50.85Q186.3-816 216-816h171q8-31 33.5-51.5T480-888q34 0 59.5 20.5T573-816h171q29.7 0 50.85 21.15Q816-773.7 816-744v528q0 29.7-21.15 50.85Q773.7-144 744-144H216Zm281-631q7-7 7-17t-7-17q-7-7-17-7t-17 7q-7 7-7 17t7 17q7 7 17 7t17-7Z"/>
                    </svg>
                  </span>

                  ${totalDelayed > 0 ? `${totalDelayed} Delayed` : ""}

                  ${totalCancelled > 0 ? `
                    ${(totalDelayed > 0) ? " • " : ""}
                    <span class="summary-cancelled">
                      ${totalCancelled} Cancelled
                    </span>
                  ` : ""}

                  ${totalDiverted > 0 ? `
                    ${(totalDelayed > 0 || totalCancelled > 0) ? " • " : ""}
                    <span class="diverted-word">
                      <svg xmlns="http://www.w3.org/2000/svg"
                           height="14"
                           viewBox="0 -960 960 960"
                           width="14"
                           fill="currentColor">
                        <path d="M320-120q-66 0-113-47t-47-113q0-66 47-113t113-47h200q33 0 56.5-23.5T600-520q0-33-23.5-56.5T520-600H280v80L160-640l120-120v80h240q66 0 113 47t47 113q0 66-47 113t-113 47H320q-33 0-56.5 23.5T240-280q0 33 23.5 56.5T320-200h360v-80l120 120-120 120v-80H320Z"/>
                      </svg>
                      ${totalDiverted} Diverted
                    </span>
                  ` : ""}

                </div>
              `);
            }

            var icon = weather.icon;
            var weatherClass = getWeatherClass(weather);
            var warnings = getWeatherWarnings(weather);
            var tempColor = getTempColor(weather.tempF);

            if (weather.temp) {
            htmlParts.push(`
              <div class="fs-weather-banner ${weatherClass}" style="--tempColor:${tempColor}">
                <span class="fs-weather-icon">${icon}</span>
                <span class="fs-weather-text">
                  ${weather.temp}
                  ${weather.windSpeed ? " • " + weather.windSpeed + " mph " + weather.windDir : ""}
                  ${weather.visibility !== null ? " • Vis " + weather.visibility + " mi" : ""}
                  ${weather.cloud ? " • CC " + weather.cloud + "%" : ""}
                  ${warnings ? " " + warnings : ""}
                </span>
              </div>
            `);
            }

            if (arrivalsAlerts.length > 0) {

              htmlParts.push(`
                <div class="fs-alert-section arrival-alert">
                ${lastUpdate ? `<div class="fs-last-update-corner">${lastUpdate}</div>` : ""}
                <div class="fs-alert-section-title">🔴 ARRIVALS</div>
              `);

              arrivalsAlerts.forEach(function(a){

                var statusClass = "";

                if (a.diverted) statusClass = "status-diverted";
                else if (a.delay >= 180) statusClass = "status-major";
                else if (a.delay >= 60) statusClass = "status-medium";
                else if (a.delay > 0) statusClass = "status-minor";

                htmlParts.push(`<div class="fs-alert-item ${statusClass}">${a.html}</div>`);

              });

              htmlParts.push(`</div>`);
            }

            if (departuresAlerts.length > 0) {

              htmlParts.push(`
                <div class="fs-alert-section departure-alert">
                ${lastUpdate ? `<div class="fs-last-update-corner">${lastUpdate}</div>` : ""}
                <div class="fs-alert-section-title">🔵 DEPARTURES</div>
              `);

              departuresAlerts.forEach(function(a){

                var statusClass = "";

                if (a.diverted) statusClass = "status-diverted";
                else if (a.delay >= 180) statusClass = "status-major";
                else if (a.delay >= 60) statusClass = "status-medium";
                else if (a.delay > 0) statusClass = "status-minor";

                htmlParts.push(`<div class="fs-alert-item ${statusClass}">${a.html}</div>`);

              });

              htmlParts.push(`</div>`);
            }

            if (arrivalsAlerts.length === 0 && departuresAlerts.length === 0) {

              htmlParts.push(`
                <div class="fs-alert success">
                  <div class="fs-success-main">All flights on time</div>
                  <div class="fs-success-date">${todayLabel}</div>
                  <div class="fs-success-date">${tomorrowLabel}</div>
                  ${lastUpdate ? `<div class="fs-last-update">${lastUpdate}</div>` : ""}
                </div>
              `);

            }

            var html = htmlParts.join("");

            var existingOverlay = document.getElementById("fs-alerts-overlay");

            if (existingOverlay) {

              var existingContent = existingOverlay.querySelector("#fs-alerts-content");

              if (existingContent) {

                existingContent.classList.add("fs-alerts-refresh");

                var scrollTop = existingContent.scrollTop;

                existingContent.innerHTML = html;

                existingContent.scrollTop = scrollTop;

                requestAnimationFrame(function(){
                  existingContent.classList.remove("fs-alerts-refresh");
                });

                return;
              }
            }

            content.innerHTML = html;
            
            setTimeout(trackAircraftForAlerts, 1200);
            sheet.appendChild(content);
            overlay.appendChild(sheet);
            document.body.appendChild(overlay);

            // =========================
            // Animate sheet up
            // =========================

            requestAnimationFrame(function () {

              sheet.style.transform = "translate3d(-50%, 0, 0)";


            });

            // =========================
            // Close on outside tap
            // =========================

            overlay.addEventListener("click", function(e) {

              if (e.target === overlay) {

                sheet.style.transform = "translate3d(-50%, 100%, 0)";

                setTimeout(function(){
                  overlay.remove();
                }, 450);

              }

            });

          }

          cleanTop();
          
          // ===============================
          // Observe DOM changes (SPA-safe)
          // ===============================

          var observer = new MutationObserver(function(mutations) {

              var tabs = document.getElementById("fs-bottom-tabs");
              if (!tabs && SHOW_FLIGHT_TABS && !window.fsReloading) {
                  window.fsReloading = true;
                  location.reload();
              }
          });
          
          observer.observe(document.documentElement, {
            childList: true,
            subtree: true
          });
          
          var alertsUpdateTimer = null;

          var flightsObserver = new MutationObserver(function() {

            if (!document.getElementById("fs-alerts-overlay")) return;

            if (alertsUpdateTimer) {
                clearTimeout(alertsUpdateTimer);
            }

            alertsUpdateTimer = setTimeout(function(){
                renderAlerts();
            }, 450);

          });

          var flightsContainer = document.getElementById("flight-container");

          if (flightsContainer) {
            flightsObserver.observe(flightsContainer, {
              childList: true,
              subtree: true,
            });
          }

          // ===============================
          // Inject CSS
          // ===============================

          var styleId = "fs_custom_style";
          var style = document.getElementById(styleId);
          if (!style) {
              style = document.createElement("style");
              style.id = styleId;
              document.head.appendChild(style);
          }
          
          style.innerHTML = `

          /* =========================================================
             SECTION 1 — HIDE ORIGINAL WEBSITE ELEMENTS
             ========================================================= */

          header.site-header.header-mobile,
          .jac-navbar,
          .fixed-triggers,
          section.page-hero.-noimage {
            display: none !important;
          }
          
          /* =========================================================
             CARD OUTER SPACING (allows glow to render)
             ========================================================= */
             
             .fs-alert,
             .fs-alert-section,
             .fs-weather-banner {
               margin: 16px auto 24px auto;
               width: calc(100% - 32px);
             }
          .fs-alert-section + .fs-alert-section {
            margin-top: 6px;
          }
          /* disable alert animations during refresh */
          .fs-alerts-refresh .fs-alert-section {
            animation: none !important;
          }
          
          /* =========================================================
             SUCCESS ALERT TEXT — AUTO THEME
             ========================================================= */
             

          /* 🌙 DARK MODE */
          @media (prefers-color-scheme: dark) {

            .fs-alert.success,
            .fs-success-main,
            .fs-success-sub {
              color: #ffffff;
            }

          }
          @media (prefers-color-scheme: light) {

            .fs-weather-banner.weather-freezing::after {

              background: linear-gradient(
                120deg,
                transparent 38%,
                rgba(255,255,255,0.55) 50%,
                transparent 62%
              );

            }

          }
          
          
          /* =========================================================
             SECTION 2 — SUCCESS ALERT (ALL FLIGHTS ON TIME)
             ========================================================= */
             /* ✨ SUCCESS GLOW ANIMATIONS */
             
             @keyframes successGlowPulse {
               0%,100% {
                 box-shadow:
                   0 0 6px rgba(0,220,140,0.35),
                   0 0 16px rgba(0,200,120,0.22),
                   inset 0 0 8px rgba(0,230,140,0.08);
               }

               50% {
                 box-shadow:
                   0 0 10px rgba(0,255,160,0.55),
                   0 0 24px rgba(0,220,140,0.30),
                   inset 0 0 12px rgba(0,240,160,0.12);
               }
             }

             @keyframes successBorderPulse {
               0%,100% { border-color: rgba(0,220,140,0.45); }
               50%     { border-color: rgba(0,255,160,0.85); }
             }

             @keyframes breatheScale {
               0%,100% { transform: scale(1); }
               50%     { transform: scale(1.005); }
             }
             
             .fs-alert.success {

               display: flex;
               flex-direction: column;
               align-items: flex-start;
               gap: 6px;

               padding: 14px 16px;
               border-radius: 16px;

               position: relative;
               overflow: visible;

               background: linear-gradient(
                 135deg,
                 rgba(0,200,120,0.28),
                 rgba(0,160,90,0.22)
               );

               border: 1px solid rgba(0,220,140,0.45);

               backdrop-filter: blur(4px) saturate(160%);
               -webkit-backdrop-filter: blur(4px) saturate(160%);

               font-weight: 800;

               transform: translateZ(0);
               
               /* gentle green breathing aura */
                /* ✨ SUCCESS GLOW ANIMATIONS */

               animation:
                 successGlowPulse 3.2s ease-in-out infinite,
                 successBorderPulse 3.2s ease-in-out infinite,
                 breatheScale 3.2s ease-in-out infinite;
             }
             .fs-alert.success::after {

               content: "";
               position: absolute;
               inset: 0;

               border-radius: 16px;

               background: linear-gradient(
                 105deg,
                 transparent 30%,
                 rgba(0,255,150,0.10) 50%,
                 transparent 70%
               );

               background-size: 200% auto;

               animation: shimmerSweep 4s linear infinite;

               pointer-events: none;
             }

             @keyframes shimmerSweep {
               0%   { background-position: -200% center; }
               100% { background-position: 200% center; }
             }
             
             .fs-success-icon {
               display: inline-flex;
               align-items: center;
             }
             
           .fs-success-main {
             font-size: 16px;
             font-weight: 900;
             letter-spacing: 0.4px;

             display: flex;
             align-items: center;
             gap: 6px;
           }

          .fs-success-sub {
            font-size: 13px;
            font-weight: 700;
            opacity: 0.85;
          }
          
          .fs-success-date {
            font-size: 13px;
            font-weight: 900;
            margin-top: 8px;
            letter-spacing: 0.5px;
            color: #111;
            text-shadow: 0 1px 1px rgba(0,0,0,0.15);
          }

          .fs-last-update {
            margin-top: 8px;
            font-size: 11px;
            opacity: 0.6;
          }
          .fs-count-row {
            font-size: 13px;
            font-weight: 700;
            display: flex;
            gap: 6px;
            align-items: center;
          }
          /* 🌙 DARK MODE — arrivals green */
          @media (prefers-color-scheme: dark) {

            .fs-arrivals {
              color: #39ffb6;
              font-weight: 900;
              text-shadow: 0 0 8px rgba(0,255,160,0.55);
            }

            .fs-label-arrivals {
              color: #39ffb6;
              opacity: 0.9;
              font-weight: 700;
            }

          }

          /* ☀️ LIGHT MODE — arrivals red */
          @media (prefers-color-scheme: light) {

            .fs-arrivals {
              color: #ff4d4d;
              font-weight: 900;
              text-shadow: none;
            }

            .fs-label-arrivals {
              color: #ff4d4d;
              opacity: 0.9;
              font-weight: 700;
            }

          }
          /* 🌙 DARK MODE — departures */
          @media (prefers-color-scheme: dark) {

            .fs-departures {
              color: #7ecbff;
              font-weight: 900;
              text-shadow: 0 0 8px rgba(120,200,255,0.6);
            }

            .fs-label-departures {
              color: #7ecbff;
              opacity: 0.9;
              font-weight: 700;
            }

          }

          /* ☀️ LIGHT MODE — stronger blue */
          @media (prefers-color-scheme: light) {

            .fs-departures {
              color: #0066ff;   /* deeper aviation blue */
              font-weight: 900;
              text-shadow: none;
            }

            .fs-label-departures {
              color: #0066ff;
              opacity: 0.9;
              font-weight: 700;
            }

          }
          /* =========================================================
             ALERT CARD — LAST UPDATE CORNER
             ========================================================= */
             .fs-last-update-corner{
               position:absolute;
               top:8px;
               right:10px;

               font-size:10px;
               font-weight:700;
               letter-spacing:0.3px;
               color:rgba(255,255,255,0.9);
               text-shadow:0 1px 2px rgba(0,0,0,0.45);
             }
          
          /* =========================================================
             LIGHT MODE — EMERALD SUCCESS GLOW
             ========================================================= */

          @media (prefers-color-scheme: light) {

            /* Darker card so glow is visible */
            .fs-alert.success {

              background: linear-gradient(
                135deg,
                rgba(0,130,85,0.40),
                rgba(0,100,65,0.34)
              );

              border: 1px solid rgba(0,140,90,0.65);

            }

            @keyframes successGlowPulse {

              0%,100% {
                box-shadow:
                  0 0 12px rgba(0,150,100,0.65),
                  0 0 28px rgba(0,150,100,0.40),
                  inset 0 0 10px rgba(0,190,130,0.30);
              }

              50% {
                box-shadow:
                  0 0 22px rgba(0,200,140,0.95),
                  0 0 48px rgba(0,200,140,0.65),
                  inset 0 0 16px rgba(0,220,160,0.45);
              }

            }

            @keyframes successBorderPulse {
              0%,100% { border-color: #009966; }
              50%     { border-color: #00cc88; }
            }

          }


          /* =========================================================
             SECTION 3 — FLIGHT ROW LAYOUT
             ========================================================= */

          .fs-flight-row {
            display: flex;
            align-items: center;
            gap: 6px;
            flex-wrap: wrap;   /* allows wrapping only if screen too small */
          }

          .fs-airline-icon img {
            width: 16px;
            height: 16px;
            object-fit: contain;
          }
          @media (prefers-color-scheme: dark) {

            .fs-airline-icon img {
              filter: invert(1) hue-rotate(180deg) !important;
            }

          }

          .fs-flight-name {
            font-weight: 900;
          }

          .fs-airport-code {
            font-size: 11px;
            font-weight: 900;
            opacity: 0.75;
          }

          .fs-delay-time {
            font-weight: 900;
          }

          .fs-plus-format {
            font-size: 11px;
            opacity: 0.75;
          }
          
           /* =========================================================
             SECTION Cancelled
             ========================================================= */
             .cancelled-word {
              
               font-weight: 900;
               letter-spacing: 0.3px;
               color: #ffd35c;
               text-shadow: 0 0 8px rgba(255,210,80,0.6);
             }

          /* =========================================================
             SECTION 4 — SUMMARY / DATE / TIMES
             ========================================================= */

          .fs-summary {
          gap: 6px;
          display: flex;
            align-items: center;
            font-size: 13px;
            font-weight: 900;
            margin-bottom: 5px;
            padding-left: 16px;
            vertical-align: middle;
          }
          /* DARK MODE */
          @media (prefers-color-scheme: dark) {
            .fs-summary {
              color: #ffffff;
            }
          }

          /* LIGHT MODE */
          @media (prefers-color-scheme: light) {
            .fs-summary {
              color: #111111;
            }
          }
          
          .fs-summary-icon svg {
            width: 16px;
            height: 16px;
          }

          /* 🌙 Dark mode — bright amber */
          @media (prefers-color-scheme: dark) {
            .fs-summary-icon svg {
              color: #ffb300;
            }
          }

          /* ☀️ Light mode — softer amber */
          @media (prefers-color-scheme: light) {
            .fs-summary-icon svg {
              color: #cc8800;
            }
          }

          .fs-alert-date {
            font-size: 11px;
            font-weight: 700;
            opacity: 0.75;
            margin-bottom: 4px;
            letter-spacing: 0.5px;
          }

          .fs-alert-times {
            margin-top: 6px;
            font-size: 11px;
            font-weight: 700;

            display: flex;
            justify-content: flex-start;
            gap: 10px;

            opacity: 0.85;
          }

          .sched-time {
            color: rgba(255,255,255,0.65);
          }

          .actual-time {
            color: #ffffff;
            font-weight: 800;
          }

          .dot {
            opacity: 0.5;
          }


          /* =========================================================
             SECTION 5 — WEATHER ICONS
             ========================================================= */

          .fs-weather-icon {
            display: flex;
            align-items: center;
            opacity: 0.9;
          }

          .fs-weather-icon svg {
            width: 18px;
            height: 18px;
            fill: currentColor;
          }


          /* =========================================================
             SECTION 6 — STATUS WORD COLORS
             ========================================================= */
             
             .status-word {
               font-weight: 900;
             }

             .diverted-word {
               color: #e879ff;
               text-shadow: 0 0 10px rgba(200,0,255,0.9);
             }

             /* icon alignment for diverted */
             .diverted-word svg {
               width: 14px;
               height: 14px;
               margin-right: 4px;
               vertical-align: -2px;
             }

             .delayed-word {
               color: #ffd35c;
             }
             
             // Delayed arrival
             .fs-aircraft-info{

               margin-top:6px;
               font-size:12px;
               font-weight:800;

               color:#7ecbff;

               display:flex;
               gap:6px;
               flex-wrap:wrap;

             }

             /* Aircraft arrival progress */

             .fs-flight-progress{

               margin-top:6px;
               width:100%;
               height:6px;

               background:rgba(255,255,255,0.15);
               border-radius:6px;

               overflow:hidden;

             }

             .fs-flight-progress-bar{

               height:100%;
               width:0%;

               background:linear-gradient(
                 90deg,
                 #4aa3ff,
                 #7ecbff
               );

               border-radius:6px;

               transition:width 0.8s ease;

             }
             
             


          /* =========================================================
             SECTION 7 — WEATHER WARNING ICONS
             ========================================================= */

          .fs-weather-warning {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            margin-left: 6px;
            vertical-align: middle;
          }

          .fs-weather-warning svg {
            width: 14px;
            height: 14px;
            flex-shrink: 0;
          }

          /* icon colors */

          .ice-warning svg { color: #7dd3ff; }
          .wind-warning svg { color: #a5f3fc; }
          .vis-warning svg { color: #ffd35c; }


          /* =========================================================
             SECTION 8 — ALERT ITEM BASE
             ========================================================= */

          .fs-alert-item {
            padding: 8px 0;
            border-bottom: 1px solid rgba(255,255,255,0.2);
            font-weight: 800;
            font-size: 14px;
            color: rgba(255,255,255,0.92);
            text-shadow: 0 1px 3px rgba(0,0,0,0.45);
          }
          @media (prefers-color-scheme: dark) {

            .fs-alert-item {
              border-bottom: 1px solid rgba(255,255,255,0.45);
              box-shadow: 0 1px 0 rgba(255,255,255,0.08);
            }

          }

          .fs-alert-item:last-child {
            border-bottom: none;
          }
          
          /* =========================================================
             SECTION 9 — WEATHER BANNER (PRO VERSION)
             ========================================================= */

          .fs-weather-banner {

            position: relative;
            overflow: hidden;

            display: flex;
            align-items: center;
            gap: 8px;

            padding: 8px 12px;
            margin-bottom: 12px;

            border-radius: 14px;

            font-weight: 800;
            font-size: 14px;

            transform: translateZ(0);

            backdrop-filter: blur(6px) saturate(140%);
            -webkit-backdrop-filter: blur(6px) saturate(140%);

            border: 1px solid color-mix(in srgb, var(--tempColor) 45%, transparent);

            background:
              linear-gradient(
                135deg,
                color-mix(in srgb, var(--tempColor) 30%, transparent),
                color-mix(in srgb, var(--tempColor) 12%, transparent)
              );

            box-shadow:
              0 0 10px color-mix(in srgb, var(--tempColor) 35%, transparent),
              inset 0 0 6px rgba(255,255,255,0.05);

            transition:
              background .4s ease,
              box-shadow .4s ease;

            animation: fsWeatherPulse 10s ease-in-out infinite;
          }


          /* =========================================================
             TEXT SCROLL
             ========================================================= */
             .fs-weather-text {

               flex: 1;

               display: block;

               white-space: nowrap;

               overflow-x: auto;
               overflow-y: hidden;

               /* larger touch area */
               padding: 8px 6px;

               /* smoother mobile scrolling */
               -webkit-overflow-scrolling: touch;

               /* prioritize horizontal gestures */
               touch-action: pan-x;

               /* make grabbing easier */
               cursor: grab;

               scrollbar-width: none;
             }

             .fs-weather-text:active {
               cursor: grabbing;
             }

             .fs-weather-text::-webkit-scrollbar {
               display: none;
             }
             .fs-weather-banner {
               touch-action: pan-x;
             }
             


          /* =========================================================
             TEMPERATURE GLOW ANIMATION
             ========================================================= */
             @keyframes fsWeatherPulse {

               0%,100% {

                 box-shadow:
                   0 0 8px color-mix(in srgb, var(--tempColor) 28%, transparent),
                   inset 0 0 6px rgba(255,255,255,0.05);

               }

               50% {

                 box-shadow:
                   0 0 14px color-mix(in srgb, var(--tempColor) 40%, transparent),
                   inset 0 0 8px rgba(255,255,255,0.06);

               }

             }


          /* =========================================================
             THEME COLORS
             ========================================================= */

          @media (prefers-color-scheme: dark) {

            .fs-weather-banner {
              color: #ffffff;
            }

          }

          @media (prefers-color-scheme: light) {

            .fs-weather-banner {
              color: #1a1a1a;
            }

          }
      
          /* =========================================================
             SECTION 11 — DELAY LEVEL COLORS
             ========================================================= */

          /* Major Delay */
          .fs-alert-item.status-major {
            color: #ffdddd;
            text-shadow: 0 0 6px rgba(255,0,0,0.6);
          }

          /* Medium Delay */
          .fs-alert-item.status-medium {
            color: #ffe4b3;
          }

          /* Minor Delay */
          .fs-alert-item.status-minor {
            color: #fff5cc;
            opacity: 0.95;
          }

          /* Diverted */
          .fs-alert-item.status-diverted {
            color: #f2d6ff;
            font-weight: 900;
            letter-spacing: 0.3px;
            text-shadow: 0 0 10px rgba(180,0,255,0.8);
          }


          /* =========================================================
             SECTION 12 — ALERT SECTIONS (ARRIVALS / DEPARTURES)
             ========================================================= */

          .fs-alert-section-title {
            font-size: 13px;
            font-weight: 900;
            letter-spacing: 1.2px;
            margin-bottom: 8px;
            text-transform: uppercase;
            color: #ffffff;
            text-shadow: 0 1px 4px rgba(0,0,0,0.4);
          }

          /* 🔴 ARRIVAL */
          .fs-alert-section.arrival-alert {
            border-radius: 16px;

            background:
              linear-gradient(
                135deg,
                rgba(255, 0, 0, 0.42),
                rgba(160, 0, 0, 0.34)
              );

            border: 1px solid rgba(255, 80, 80, 0.55);

            box-shadow:
              inset 0 0 8px 6px rgba(180, 0, 0, 0.22);
          }

          /* 🔵 DEPARTURE */
          .fs-alert-section.departure-alert {
            border-radius: 16px;

            background:
              linear-gradient(
                145deg,
                rgba(0,120,255,0.40),
                rgba(0,70,200,0.32)
              );

            border: 1px solid rgba(120,180,255,0.35);

            box-shadow:
              inset 0 0 8px 6px rgba(0,80,200,0.22);
          }


          /* =========================================================
             SECTION 13 — ALERT ANIMATIONS
             ========================================================= */
             
             @keyframes alertFloat {
               0% {
                 transform: scale(1.015);
               }

               100% {
                 transform: scale(1);
               }
             }
             
             .fs-alert-section {
               padding: 10px 12px;
               border-radius: 14px;
               margin-bottom: 8px;
               position: relative;
               overflow: visible;
               border: 1px solid rgba(255,255,255,0.25);

               transform: translateZ(0);
             }
             .fs-alert-section {
               transform: translateZ(0) scale(1);
             }

             .flight-toggle {
               display: none !important;
             }
     

          /* =========================================================
             SECTION 14 — WAVE ENTRY ANIMATION
             ========================================================= */

          @keyframes waveWobbleEntry {

            0% {
              transform: translateX(-50%) translateY(70px) scale(0.96);
              opacity: 0;
            }

            100% {
              transform: translateX(-50%) translateY(0) scale(1);
              opacity: 1;
            }

          }


          /* =========================================================
             SECTION 15 — BOTTOM TABS CONTAINER
             ========================================================= */

          #fs-bottom-tabs {

            will-change: transform;
            isolation: isolate;

            position: fixed;
            overflow: hidden;

            bottom: calc(env(safe-area-inset-bottom, 0px) + 24px);
            left: 50%;
            transform: translateX(-50%);

            width: 260px;
            height: 64px;

            display: flex;
            justify-content: space-around;
            align-items: center;

            padding: 2px 2px;
            border-radius: 999px;

            background: linear-gradient(
              135deg,
              rgba(255,255,255,0.18),
              rgba(255,255,255,0.06)
            );

            backdrop-filter: blur(3px) saturate(160%);
            -webkit-backdrop-filter: blur(3px) saturate(160%);

            border: 1px solid rgba(255,255,255,0.25);

            box-shadow:
              0 6px 18px rgba(0,0,0,0.18),
              inset 0 1px 1px rgba(255,255,255,0.3);

            z-index: 2147483647;

            transition: background 0.35s ease, transform 0.25s ease;

            animation: waveWobbleEntry 0.9s cubic-bezier(.33,1,.68,1) forwards;
          }
          
          


          /* =========================================================
             SECTION 16 — BOTTOM TAB PILL INDICATOR
             ========================================================= */

          #fs-bottom-tabs::before {

            content: "";
            position: absolute;

            top: 3px;
            left: 3px;

            width: calc(33.333% - 2px);
            height: 58px;

            background: rgba(255,255,255,0.18);

            border-radius: 999px;

            transition: transform 0.45s cubic-bezier(.22,1,.36,1);

            pointer-events: none;
          }

          #fs-bottom-tabs.subpage::before {
            width: calc(50% - 4px);
          }

          #fs-bottom-tabs.arrivals::before { transform: translateX(0%); }
          #fs-bottom-tabs.departures::before { transform: translateX(100%); }
          #fs-bottom-tabs.alerts::before { transform: translateX(200%); }


          /* =========================================================
             SECTION 17 — TAB BUTTONS
             ========================================================= */

          .fs-tab {

            flex: 1;
            height: 58px;

            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;

            font-size: 12px;
            font-weight: 600;

            color: #888;

            cursor: pointer;
            border-radius: 999px;

            transition: all 0.25s cubic-bezier(.4,0,.2,1);
          }

          .fs-tab span {
            display: block;
            font-size: 18px;
            margin-bottom: 2px;
          }

          .fs-tab:active {
            transform: scale(0.94);
          }
          

          .fs-tab.active:not(:active) {
            color: #fff;
            transform: scale(1.00);
            box-shadow: 0 4px 15px rgba(0,0,0,0.25);
          }
          .arrow {
            transition: transform 0.25s ease;
          }

          .arrow.rotate {
            transform: rotate(180deg);
          }


          /* =========================================================
             SECTION 18 — TAB COLOR STATES
             ========================================================= */

          /* ARRIVALS */

          #fs-bottom-tabs.arrivals {
            background: linear-gradient(
              135deg,
              rgba(255,0,0,0.38),
              rgba(200,0,0,0.28)
            );
          }

          .fs-tab.arrivals.active {
            background: linear-gradient(
              135deg,
              rgba(255,0,0,0.90),
              rgba(180,0,0,0.80)
            );
            color: #ffffff;
          }


          /* DEPARTURES */

          #fs-bottom-tabs.departures {
            background: linear-gradient(
              135deg,
              rgba(0,120,255,0.38),
              rgba(0,80,200,0.28)
            );
          }

          .fs-tab.departures.active {
            background: linear-gradient(
              135deg,
              rgba(0,120,255,0.90),
              rgba(0,70,200,0.80)
            );
            color: #ffffff;
          }


          /* ALERTS */

          #fs-bottom-tabs.alerts {
            background: linear-gradient(
              135deg,
              rgba(255,200,0,0.35),
              rgba(255,170,0,0.25)
            );
            backdrop-filter: blur(3px) saturate(180%);
            -webkit-backdrop-filter: blur(3px) saturate(180%);
          }

          .fs-tab.alerts.active {
            background: linear-gradient(
              135deg,
              rgba(255,200,0,0.95),
              rgba(255,150,0,0.85)
            );
            color: #ffffff;
          }


          /* =========================================================
             SECTION 19 — SUBPAGE BACK PILL
             ========================================================= */

          .fs-tab.subpage-pill[data-type="sub-back"] {

            background: linear-gradient(
              135deg,
              rgba(150,90,255,0.45),
              rgba(100,60,220,0.35)
            );

            color: #fff;
          }

          .fs-tab.subpage-pill[data-type="sub-back"].active {

            background: linear-gradient(
              135deg,
              rgba(150,90,255,0.75),
              rgba(100,60,220,0.65)
            );
          }
          
          

          `;

         
          // ===============================
          // Bottom Tabs Logic (STABLE)
          // ===============================

          var existing = document.getElementById("fs-bottom-tabs");

          if (!SHOW_FLIGHT_TABS) {
              if (existing) existing.remove();
              return;
          }

          if (!existing) {

              var tabBar = document.createElement("div");
              tabBar.id = "fs-bottom-tabs";

              if (IS_FLIGHTS_MAIN) {

                  tabBar.classList.add("arrivals");
                  
                  tabBar.innerHTML = `
                    <div class="fs-tab arrivals active" data-type="arrivals">
                      <span class="icon">
                        <svg xmlns="http://www.w3.org/2000/svg"
                             height="22"
                             viewBox="0 -960 960 960"
                             width="22"
                             fill="currentColor">
                          <path d="M120-120v-80h720v80H120Zm622-202L120-499v-291l96 27 48 139 138 39-35-343 115 34 128 369 172 49q25 8 41.5 29t16.5 48q0 35-28.5 61.5T742-322Z"/>
                        </svg>
                      </span>
                      Arrivals
                    </div>

                    <div class="fs-tab departures" data-type="departures">
                      <span class="icon">
                        <svg xmlns="http://www.w3.org/2000/svg"
                             height="22"
                             viewBox="0 -960 960 960"
                             width="22"
                             fill="currentColor">
                          <path d="M120-120v-80h720v80H120Zm70-200L40-570l96-26 112 94 140-37-207-276 116-31 299 251 170-46q32-9 60.5 7.5T864-585q9 32-7.5 60.5T808-487L190-320Z"/>
                        </svg>
                      </span>
                      Departures
                    </div>
                    
                   <div class="fs-tab alerts" data-type="alerts">
                     <span class="icon">
                       <svg xmlns="http://www.w3.org/2000/svg"
                             height="22"
                             viewBox="0 -960 960 960"
                             width="22"
                             fill="currentColor">
                       <path d="M508.5-291.5Q520-303 520-320t-11.5-28.5Q497-360 480-360t-28.5 11.5Q440-337 440-320t11.5 28.5Q463-280 480-280t28.5-11.5ZM440-440h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Z"/>                       </svg>
                      </span>
                      Alerts
                    </div>
                  `;

              } else {
              
              tabBar.classList.add("subpage");
              tabBar.innerHTML = `
              
              <div class="fs-tab subpage-pill active" data-type="sub-back">
                <span class="icon">
                  <svg xmlns="http://www.w3.org/2000/svg"
                       height="22"
                       viewBox="0 -960 960 960"
                       width="22"
                       fill="currentColor">
                    <path d="M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z"/>
                  </svg>
                </span>
                Back
              </div>
              
              <div class="fs-tab subpage-pill" data-type="sub-toggle">
                <span class="icon">
                  <svg class="arrow"
                       xmlns="http://www.w3.org/2000/svg"
                       height="22"
                       viewBox="0 -960 960 960"
                       width="22"
                       fill="currentColor">
                    <path d="M480-528 296-344l-56-56 240-240 240 240-56 56-184-184Z"/>
                  </svg>
                </span>
                <span class="toggle-text">Top</span>
              </div>
              `;
              }

              document.documentElement.appendChild(tabBar);
              
              window.addEventListener("scroll", function() {

                var max = document.body.scrollHeight - window.innerHeight;
                if (max <= 0) return;

                var ratio = window.scrollY / max;

                var blur = 3 + (ratio * 2);
                 tabBar.style.backdropFilter =
                "blur(" + blur + "px) saturate(160%)";
              });
              
              var toggleBtn = tabBar.querySelector('[data-type="sub-toggle"]');
              if (toggleBtn) {

                var arrow = toggleBtn.querySelector('.arrow');
                var toggleText = toggleBtn.querySelector('.toggle-text');

                function updateToggleState() {

                    var current = window.scrollY;
                    var maxScroll = document.body.scrollHeight - window.innerHeight;

                    if (current > maxScroll / 2) {

                        // Arrow UP (go to top)
                        arrow.classList.remove("rotate");
                        toggleText.textContent = "Top";

                    } else {

                        // Arrow DOWN (go to bottom)
                        arrow.classList.add("rotate");
                        toggleText.textContent = "Bottom";
                    }
                }

                updateToggleState();
                window.addEventListener("scroll", updateToggleState);
              }

              tabBar.addEventListener("click", function(e) {

                  var tab = e.target.closest(".fs-tab");
                  if (!tab) return;

                  if (tab.dataset.type === "sub-back") {
                      window.location.href = "https://www.jacksonholeairport.com/flights/";
                      return;
                  }
                  
                  
                  if (tab.dataset.type === "sub-toggle") {

                      var current = window.scrollY;
                      var maxScroll = document.body.scrollHeight - window.innerHeight;

                      if (current > maxScroll / 2) {

                          window.scrollTo({
                              top: 0,
                              behavior: "smooth"
                          });

                      } else {

                          window.scrollTo({
                              top: document.body.scrollHeight,
                              behavior: "smooth"
                          });
                      }

                      return;
                  }

                  document.querySelectorAll(".fs-tab").forEach(function(t){
                      t.classList.remove("active");
                  });

                  tab.classList.add("active");
                  
                  if (tab.dataset.type === "alerts") {

                    tabBar.classList.remove("arrivals","departures");
                    tabBar.classList.add("alerts");

                    document.querySelectorAll(".flight-table-wrap").forEach(function(el){
                        el.style.display = "";
                    });

                    renderAlerts(); // this only triggers sheet opening now

                    return;
                  }
                  
                  if (tab.dataset.type === "arrivals") {

                      tabBar.classList.remove("departures","alerts");
                      tabBar.classList.add("arrivals");

                      document.querySelectorAll(".flight-table-wrap").forEach(function(el){
                          el.style.display = "";
                      });

                      var alertWrapper = document.getElementById("fs-alerts-overlay");
                      if (alertWrapper) alertWrapper.remove();

                      var btn = document.querySelector('li[data-target="hide-departures"]');
                      if (btn) btn.click();

                  }
                  else if (tab.dataset.type === "departures") {

                      tabBar.classList.remove("arrivals","alerts");
                      tabBar.classList.add("departures");

                      document.querySelectorAll(".flight-table-wrap").forEach(function(el){
                          el.style.display = "";
                      });

                      var alertWrapper = document.getElementById("fs-alerts-overlay");
                      if (alertWrapper) alertWrapper.remove();

                      var btn = document.querySelector('li[data-target="hide-arrivals"]');
                      if (btn) btn.click();
                  }

                  cleanTop();
              });
          }

        })();
        """.trimIndent(),
            null
        )
    }
}