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
          
          function generateSmartReason(delayMinutes, airline, flight) {

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
          
          function renderAlerts() {

            var container = document.getElementById("flight-container");
            if (!container) return;

            var tables = container.querySelectorAll("table.jha-flights");
            
            var arrivalsAlerts = [];
            var departuresAlerts = [];

            var arrivalsMajor = 0;
            var departuresMajor = 0;

            tables.forEach(function(table, index) {

              var rows = table.querySelectorAll("tbody tr");
              
              rows.forEach(function(row) {

                  var statusCell = row.querySelector(".status span");
                  if (!statusCell) return;

                  var statusLower = statusCell.textContent.trim().toLowerCase();

                  var airline = row.querySelector(".airline")?.innerText || "";
                  var flight = row.querySelector(".flight")?.innerText || "";
                  var sched = row.querySelector(".sched")?.innerText || "";
                  var actual = row.querySelector(".actual")?.innerText || "";

                  if (!statusLower.includes("delay")) return;

                  var delayMinutes = calculateDelayMinutes(sched, actual);

                  // 🔴 Count major disruptions (180+)
                  if (delayMinutes >= 180) {
                      if (index === 0) arrivalsMajor++;
                      if (index === 1) departuresMajor++;
                  }

                  var reason = generateSmartReason(delayMinutes, airline, flight);

                  var message = airline + " " + flight;

                  if (delayMinutes > 0) {
                      message += " delayed " + delayMinutes + " min";
                  } else {
                      message += " delayed";
                  }

                  message += "<div class='fs-delay-reason'>" + reason + "</div>";

                  // 🟥 Arrivals
                  if (index === 0) {
                      arrivalsAlerts.push({
                          html: message,
                          delay: delayMinutes
                      });
                  }

                  // 🟦 Departures
                  else if (index === 1) {
                      departuresAlerts.push({
                          html: message,
                          delay: delayMinutes
                      });
                  }
              });

            });
            
            arrivalsAlerts.sort(function(a, b) {
                return b.delay - a.delay;
            });

            departuresAlerts.sort(function(a, b) {
                return b.delay - a.delay;
            });

            showAlertsUI(arrivalsAlerts, departuresAlerts);
          }
          
          function showAlertsUI(arrivalsAlerts, departuresAlerts) {

            var old = document.getElementById("fs-alerts-wrapper");
            if (old) old.remove();

            var wrapper = document.createElement("div");
            wrapper.id = "fs-alerts-wrapper";
            
            wrapper.style.position = "fixed";
            wrapper.style.left = "50%";
            wrapper.style.transform = "translateX(-50%)";
            wrapper.style.width = "92%";
            wrapper.style.maxWidth = "520px";
            wrapper.style.zIndex = "999999";
            wrapper.style.pointerEvents = "auto";
            
            wrapper.style.bottom = "110px";

            var panel = document.createElement("div");
            panel.id = "fs-alerts-panel";
            
            var html = "";

            // 🔴 ARRIVALS
            if (arrivalsAlerts.length > 0) {

              html += `<div class="fs-alert-section arrival-alert">`;

              html += `
                <div class="fs-alert-section-title">
                  🔴 ARRIVALS
                </div>
              `;

              arrivalsAlerts.forEach(function(a) {
                html += `<div class="fs-alert-item">${a.html}</div>`;
              });

              html += `</div>`;
            }

            // 🔵 DEPARTURES
            if (departuresAlerts.length > 0) {

              html += `<div class="fs-alert-section departure-alert">`;

              html += `
                <div class="fs-alert-section-title">
                  🔵 DEPARTURES
                </div>
              `;

              departuresAlerts.forEach(function(a) {
                html += `<div class="fs-alert-item">${a.html}</div>`;
              });

              html += `</div>`;
            }

            panel.innerHTML = html;
            wrapper.appendChild(panel);
            
            wrapper.style.transform = "translate3d(-50%, 24px, 0)";
            wrapper.style.willChange = "transform";
            wrapper.style.transition =
            "transform 0.55s cubic-bezier(.22,1,.36,1)";

            document.body.appendChild(wrapper);

            wrapper.offsetHeight;
            
            requestAnimationFrame(function() {
                wrapper.style.transform = "translate3d(-50%, 0, 0)";
            });
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

          cleanTop();
          
          // ===============================
          // Observe DOM changes (SPA-safe)
          // ===============================

          var observer = new MutationObserver(function(mutations) {

              var tabs = document.getElementById("fs-bottom-tabs");

              if (!tabs && SHOW_FLIGHT_TABS) {
                  // Page content changed → recreate tabs
                  setTimeout(function() {
                      console.log("Re-injecting tabs after DOM change");
                      location.reload(); // TEMP (we'll improve below)
                  }, 50);
              }
          });

          observer.observe(document.body, {
              childList: true,
              subtree: true
          });

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
            header.site-header.header-mobile,
            .jac-navbar,
            .fixed-triggers,
            section.page-hero.-noimage {
              display: none !important;
            }
            
            .fs-delay-reason {
              margin-top: 6px;
              font-size: 12px;
              font-weight: 500;
              color: rgba(255,255,255,0.85);
              opacity: 1;
            }

            .fs-alert.warning {
              background: rgba(255,180,0,0.18);
              border: 1px solid rgba(255,180,0,0.4);
            }

            .fs-alert.success {
              background: rgba(0,200,120,0.18);
              border: 1px solid rgba(0,200,120,0.4);
            }
            
            /* ===============================
               ALERT SECTIONS (NEW STRUCTURE)
               =============================== */

            .fs-alert-section {
              padding: 10px 12px; 
              border-radius: 14px;
              margin-bottom: 8px;
              backdrop-filter: blur(3px);
              backdrop-filter: blur(3px) saturate(180%);
              -webkit-backdrop-filter: blur(3px) saturate(180%);
              border: 1px solid rgba(255,255,255,0.25);
              animation: alertFloat 0.4s ease-out;
            }
            
            .fs-alert-section-title {
              font-size: 13px;
              font-weight: 900;
              letter-spacing: 1.2px;
              margin-bottom: 8px;
              text-transform: uppercase;
              color: #ffffff;
              text-shadow: 0 1px 4px rgba(0,0,0,0.4);
            }
            
            .fs-alert-item {
              padding: 8px 0;
              border-bottom: 1px solid rgba(255,255,255,0.2);
              font-weight: 800;
              font-size: 14px;
              color: #ffffff;
              text-shadow: 0 1px 3px rgba(0,0,0,0.45);
            }

            .fs-alert-item:last-child {
              border-bottom: none;
            }

            /* Arrival section tint */
            
            /* 🔴 ARRIVAL — INTENSE RED */
            .fs-alert-section.arrival-alert {
              background: linear-gradient(
                135deg,
                rgba(255,0,0,0.55),
                rgba(180,0,0,0.45)
              );
              border: 1px solid rgba(255,0,0,0.9);
              box-shadow:
                0 10px 30px rgba(255,0,0,0.35),
                inset 0 1px 0 rgba(255,255,255,0.3);
            }

            /* 🔵 DEPARTURE — INTENSE BLUE */
            .fs-alert-section.departure-alert {
              background: linear-gradient(
                135deg,
                rgba(0,120,255,0.55),
                rgba(0,60,200,0.45)
              );
              border: 1px solid rgba(0,140,255,0.9);
              box-shadow:
                0 10px 30px rgba(0,120,255,0.35),
                inset 0 1px 0 rgba(255,255,255,0.3);
            }

            .arrival-alert {
              background: rgba(255,80,80,0.18);
              border: 1px solid rgba(255,80,80,0.45);
            }

            .departure-alert {
              background: rgba(0,120,255,0.18);
              border: 1px solid rgba(0,120,255,0.45);
            }

            @keyframes alertFloat {
              from { opacity: 0; transform: translateY(12px); }
              to { opacity: 1; transform: translateY(0); }
            }

            .flight-toggle {
              display: none !important;
            }
            
            
             /* 🌊 WAVE WOBBLE ENTRY ANIMATION */
             
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
           
           @keyframes alertSlideUp {
             0% {
               opacity: 0;
               transform: translateX(-50%) translateY(28px) scale(0.985);
               filter: blur(4px);
             }

             60% {
               opacity: 1;
               transform: translateX(-50%) translateY(4px) scale(1.002);
               filter: blur(0);
             }

             100% {
               transform: translateX(-50%) translateY(0) scale(1);
             }
           }


            #fs-bottom-tabs {
            will-change: transform;
            isolation: isolate;
              position: fixed;
              overflow: hidden;
              bottom: calc(env(safe-area-inset-bottom, 0px) + 24px);
              left: 50%;
              transform: translateX(-50%);
              width: 240px;
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
              backdrop-filter: blur(6px) saturate(160%);
              -webkit-backdrop-filter: blur(6px) saturate(160%);
              border: 1px solid rgba(255,255,255,0.25);
              box-shadow:
                0 6px 18px rgba(0,0,0,0.18),
                inset 0 1px 1px rgba(255,255,255,0.3);
              z-index: 2147483647;
              transition: background 0.35s ease, transform 0.25s ease;
              
              /* 🌊 ADD WAVE WOBBLE ON ENTRY */
              animation: waveWobbleEntry 0.9s cubic-bezier(.33,1,.68,1) forwards;
            }
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
            /* When subpage has 2 pills */
            #fs-bottom-tabs.subpage::before {
              width: calc(50% - 4px);
            }
            
            #fs-bottom-tabs.subpage .fs-tab.subpage-pill.active {
              color: #fff;
            }

            #fs-bottom-tabs.subpage::before {
              transform: translateX(0%);
            }

            #fs-bottom-tabs.subpage .fs-tab[data-type="sub-toggle"].active ~ ::before {
              transform: translateX(100%);
            }
            #fs-bottom-tabs.arrivals::before {
              transform: translateX(0%);
            }

            #fs-bottom-tabs.departures::before {
              transform: translateX(100%);
            }

            #fs-bottom-tabs.alerts::before {
              transform: translateX(200%);
            }
            
            /* 🟡 ALERTS MAIN PAGE */
            #fs-bottom-tabs.alerts {
              background: linear-gradient(
                135deg,
                rgba(255,200,0,0.35),
                rgba(255,170,0,0.25)
              );
              backdrop-filter: blur(12px) saturate(180%);
              -webkit-backdrop-filter: blur(12px) saturate(180%);
            }

            .fs-tab.alerts.active {
              background: linear-gradient(
                135deg,
                rgba(255,200,0,0.85),
                rgba(255,150,0,0.75)
              );
              color: #fff;
            }
            
            .fs-tab {
              transition: transform 0.15s cubic-bezier(.3,1.5,.5,1);
            }

            .fs-tab:active {
              transform: scale(0.94);
            }

            /* active state when NOT pressed */
            .fs-tab.active:not(:active) {
              color: #fff;
              transform: scale(1.00);
              box-shadow: 0 4px 15px rgba(0,0,0,0.25);
            }
            .arrow {
              transition: transform 0.25s cubic-bezier(.4,0,.2,1);
            }
            #fs-bottom-tabs,
            #fs-bottom-tabs * {
              -webkit-tap-highlight-color: transparent !important;
              tap-highlight-color: transparent !important;
            }

            .arrow.rotate {
              transform: rotate(180deg);
            }
            
            .fs-tab {
              flex: 1;
              height: 58px;          /* 👈 pill height */
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
            
            /* === ARRIVALS / DEPARTURES / ALERTS MAIN PAGE === */

            /* 🔴 ARRIVALS BAR */
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

            /* 🔵 DEPARTURES BAR */
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

            /* 🟡 ALERTS BAR */
            #fs-bottom-tabs.alerts {
              background: linear-gradient(
                135deg,
                rgba(255,200,0,0.38),
                rgba(255,160,0,0.28)
              );
            }

            .fs-tab.alerts.active {
              background: linear-gradient(
                135deg,
                rgba(255,200,0,0.95),
                rgba(255,150,0,0.85)
              );
              color: #ffffff;
            }

            /* ===========================
               SUBPAGE — BACK PILL TINT
               =========================== */
               
               /* === Colored Back Pill === */

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
                         <path d="M771.5-531.5Q760-543 760-560t11.5-28.5Q783-600 800-600t28.5 11.5Q840-577 840-560t-11.5 28.5Q817-520 800-520t-28.5-11.5ZM760-640v-200h80v200h-80ZM247-527q-47-47-47-113t47-113q47-47 113-47t113 47q47 47 47 113t-47 113q-47 47-113 47t-113-47ZM40-160v-112q0-34 17.5-62.5T104-378q62-31 126-46.5T360-440q66 0 130 15.5T616-378q29 15 46.5 43.5T680-272v112H40Z"/>
                       </svg>
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

                var blur = 6 + (ratio * 2);
                 tabBar.style.backdropFilter =
                "blur(" + blur + "px) saturate(160%)";

                tabBar.style.backdropFilter =
                  "blur(" + blur + "px) saturate(160%)";

                tabBar.style.background =
                  "linear-gradient(135deg, rgba(255,255,255," + opacity + "), rgba(255,255,255,0.06))";
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

                      //  alerts
                      document.querySelectorAll(".flight-table-wrap").forEach(function(el){
                          el.style.display = "";
                      });

                      renderAlerts();
                      return;
                  }
                  
                  if (tab.dataset.type === "arrivals") {

                      tabBar.classList.remove("departures","alerts");
                      tabBar.classList.add("arrivals");

                      document.querySelectorAll(".flight-table-wrap").forEach(function(el){
                          el.style.display = "";
                      });

                      var alertWrapper = document.getElementById("fs-alerts-wrapper");
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

                      var alertWrapper = document.getElementById("fs-alerts-wrapper");
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