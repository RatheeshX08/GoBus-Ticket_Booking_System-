// Standalone Frontend Client Application for GoBus Bus Ticket Booking

const API_BASE_URL = 'http://localhost:8080/api';
let BUSES = [];

let selectedBusId = 'BUS-101';
let selectedSeats = new Map(); // seatId -> price
let currentBookings = [];

// Initialize on page load
window.addEventListener('DOMContentLoaded', async () => {
  const session = GoBusAuth.getSession();
  document.getElementById('logout-btn').addEventListener('click', GoBusAuth.logout);
  document.getElementById('user-greeting').textContent = `Hi, ${session.name.split(' ')[0]}`;
  try {
    await apiRequest('/health');
    await loadBuses();
    currentBookings = await apiRequest('/bookings');
    setApiStatus('Connected to the Java API and MySQL database.', false);
  } catch (error) {
    setApiStatus(
      session.demoOnly
        ? 'Demo-only mode: the GoBus API is offline. Bus search and booking require the backend.'
        : error.message,
      true
    );
  }
  renderBuses();
  renderSeatMap();
  updateFareSummary();
  updateBookingsBadge();
  if (window.location.hash === '#bookings-drawer') openMyBookings();
});

async function apiRequest(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers
      }
    });
  } catch {
    throw new Error('Cannot reach the Java API at localhost:8080. Start the backend and verify its database settings.');
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.error || `API request failed (${response.status}).`);
  }
  return body;
}

function setApiStatus(message, isError) {
  const status = document.getElementById('api-status');
  status.textContent = message;
  status.classList.toggle('error', isError);
  status.hidden = false;
}

async function loadBuses() {
  const params = new URLSearchParams({
    origin: document.getElementById('from').value,
    destination: document.getElementById('to').value
  });
  BUSES = await apiRequest(`/buses?${params}`);
  if (!BUSES.some(bus => bus.id === selectedBusId)) {
    selectedBusId = BUSES[0]?.id || '';
    selectedSeats.clear();
  }
  document.getElementById('buses-found').textContent = BUSES.length;
  const lowestFare = BUSES.length ? Math.min(...BUSES.map(bus => Number(bus.baseFare))) : 0;
  document.getElementById('lowest-fare').textContent = `₹${lowestFare}`;
}

function getActiveBus() {
  return BUSES.find(b => b.id === selectedBusId) || BUSES[0];
}

function swapCities() {
  const fromEl = document.getElementById('from');
  const toEl = document.getElementById('to');
  const temp = fromEl.value;
  fromEl.value = toEl.value;
  toEl.value = temp;
}

function setDate(dateStr, chipType) {
  document.getElementById('date').value = dateStr;
  document.getElementById('date-label').textContent = chipType === 'today' ? '(Today)' : '(Tomorrow)';
  document.getElementById('chip-today').classList.toggle('active', chipType === 'today');
  document.getElementById('chip-tomorrow').classList.toggle('active', chipType === 'tomorrow');
}

async function handleSearch(e) {
  e.preventDefault();
  try {
    await loadBuses();
    renderBuses();
    renderSeatMap();
    updateFareSummary();
    setApiStatus(`${BUSES.length} buses loaded from the database.`, false);
  } catch (error) {
    setApiStatus(error.message, true);
  }
}

function sortBy(type) {
  document.querySelectorAll('.sort-btn').forEach(btn => btn.classList.remove('active'));
  if (event && event.target) event.target.classList.add('active');

  if (type === 'price') {
    BUSES.sort((a, b) => a.baseFare - b.baseFare);
  } else if (type === 'rating') {
    BUSES.sort((a, b) => b.rating - a.rating);
  } else if (type === 'departure') {
    BUSES.sort((a, b) => a.departure.localeCompare(b.departure));
  }
  renderBuses();
}

function selectBus(id) {
  selectedBusId = id;
  selectedSeats.clear();
  renderBuses();
  renderSeatMap();
  updateFareSummary();
}

function renderBuses() {
  const container = document.getElementById('bus-list');
  if (!container) return;
  if (BUSES.length === 0) {
    container.innerHTML = '<p class="empty-state">No buses found for this route. Search another route or add buses to the database.</p>';
    return;
  }

  container.innerHTML = BUSES.map(bus => {
    const isSelected = bus.id === selectedBusId;
    return `
      <article class="bus-card ${isSelected ? 'active-bus' : ''}" onclick="selectBus('${bus.id}')">
        <div class="bus-grid">
          <div class="bus-operator">
            <h3>${bus.operator}</h3>
            <p>${bus.busType}</p>
          </div>
          <div style="text-align:center;">
            <span class="score">⭐ ${bus.rating}</span>
            <p style="font-size:11px;color:#94a3b8;margin:2px 0 0;">${bus.ratingCount} reviews</p>
          </div>
          <div class="bus-timing">
            <strong>${bus.departure} → ${bus.arrival}</strong>
            <p>${bus.duration}</p>
          </div>
          <div class="bus-price" style="text-align:right;">
            <s>₹${bus.baseFare + 200}</s>
            <span class="now">₹${bus.baseFare}</span>
          </div>
        </div>
      </article>
    `;
  }).join('');
}

function renderSeatMap() {
  const bus = getActiveBus();
  if (!bus) {
    document.getElementById('seat-map-title').textContent = 'Seat Layout';
    document.getElementById('lower-seats').innerHTML = '';
    document.getElementById('upper-seats').innerHTML = '';
    document.getElementById('seats-left').textContent = '0';
    return;
  }
  document.getElementById('seat-map-title').textContent = `Seat Layout: ${bus.operator}`;
  const allSeats = [...bus.lowerSeats, ...bus.upperSeats];
  document.getElementById('seats-left').textContent = allSeats.filter(seat => seat.status === 'available').length;

  const renderGrid = (seats) => {
    return seats.map(s => {
      const isSelected = selectedSeats.has(s.id);
      return `
        <button 
          type="button" 
          class="seat-btn ${s.status === 'sold' ? 'sold' : ''} ${isSelected ? 'selected' : ''}"
          ${s.status === 'sold' ? 'disabled' : ''}
          onclick="toggleSeat('${s.id}', ${s.price}, '${s.status}')"
        >
          <strong>${s.id}</strong>
          <span>${s.status === 'sold' ? 'Sold' : '₹' + s.price}</span>
        </button>
      `;
    }).join('');
  };

  document.getElementById('lower-seats').innerHTML = renderGrid(bus.lowerSeats);
  document.getElementById('upper-seats').innerHTML = renderGrid(bus.upperSeats);
}

function toggleSeat(id, price, status) {
  if (status === 'sold') return;
  if (selectedSeats.has(id)) {
    selectedSeats.delete(id);
  } else {
    selectedSeats.set(id, price);
  }
  renderSeatMap();
  updateFareSummary();
}

function updateFareSummary() {
  let baseFare = 0;
  selectedSeats.forEach(price => baseFare += price);

  const discount = selectedSeats.size > 0 ? 100 : 0;
  const taxable = Math.max(0, baseFare - discount);
  const gst = Math.round(taxable * 0.05);
  const total = taxable + gst;

  document.getElementById('lbl-seats').textContent = selectedSeats.size > 0 
    ? Array.from(selectedSeats.keys()).join(', ') 
    : 'None';
  document.getElementById('lbl-base').textContent = `₹${baseFare}`;
  document.getElementById('lbl-gst').textContent = `₹${gst}`;
  document.getElementById('lbl-total').textContent = `₹${total}`;
  document.getElementById('total-sub').textContent = `${selectedSeats.size} seat(s) selected`;

  const btn = document.getElementById('btn-continue');
  btn.disabled = selectedSeats.size === 0;
}

function openCheckout() {
  if (selectedSeats.size === 0) return;
  const bus = getActiveBus();
  if (!bus) return;
  const from = document.getElementById('from').value;
  const to = document.getElementById('to').value;
  const date = document.getElementById('date').value;

  document.getElementById('modal-summary').innerHTML = `
    <strong>${bus.operator} (${bus.busType})</strong><br>
    ${from} → ${to} · Date: ${date} · Dep: ${bus.departure}<br>
    <span style="color:#e11d48;font-weight:600;">Seats: ${Array.from(selectedSeats.keys()).join(', ')} · Total: ${document.getElementById('lbl-total').textContent}</span>
  `;
  document.getElementById('checkout-modal').style.display = 'flex';
}

function closeCheckout() {
  document.getElementById('checkout-modal').style.display = 'none';
}

async function confirmBooking(e) {
  e.preventDefault();
  const bus = getActiveBus();
  if (!bus) return;
  const button = document.getElementById('btn-confirm-submit');
  button.disabled = true;
  try {
    const newBooking = await apiRequest('/bookings', {
      method: 'POST',
      body: JSON.stringify({
        busId: bus.id,
        journeyDate: document.getElementById('date').value,
        seatNumbers: Array.from(selectedSeats.keys()),
        passenger: {
          name: document.getElementById('p-name').value,
          age: Number(document.getElementById('p-age').value),
          gender: document.getElementById('p-gender').value,
          phone: document.getElementById('p-phone').value,
          email: document.getElementById('p-email').value
        }
      })
    });
    currentBookings.unshift(newBooking);
    selectedSeats.clear();
    closeCheckout();
    await loadBuses();
    renderBuses();
    renderSeatMap();
    updateFareSummary();
    updateBookingsBadge();
    setApiStatus(`Booking confirmed in MySQL. PNR: ${newBooking.pnr}`, false);
    alert(`Booking Confirmed! Your PNR is ${newBooking.pnr}. Check "My Bookings" to view or download.`);
  } catch (error) {
    setApiStatus(error.message, true);
  } finally {
    button.disabled = false;
  }
}

function updateBookingsBadge() {
  document.getElementById('booking-badge').textContent = currentBookings.length;
}

function openMyBookings() {
  const container = document.getElementById('drawer-content');
  if (currentBookings.length === 0) {
    container.innerHTML = '<p style="text-align:center;color:#64748b;padding:40px 0;">No active bookings found.</p>';
  } else {
    container.innerHTML = currentBookings.map(b => `
      <div class="booking-item">
        <div style="display:flex;justify-content:space-between;border-bottom:1px solid #e2e8f0;padding-bottom:6px;margin-bottom:8px;">
          <span style="font-weight:700;font-size:14px;">PNR: ${b.pnr}</span>
          <span style="color:#15803d;font-weight:600;font-size:12px;">${b.status || 'CONFIRMED'}</span>
        </div>
        <div style="font-size:13px;font-weight:600;">${b.operator}</div>
        <div style="font-size:12px;color:#64748b;margin:4px 0;">${b.route}</div>
        <div style="font-size:12px;">Traveler: <strong>${b.passenger}</strong> · Seats: <strong>${b.seats.join(', ')}</strong></div>
        <div style="font-size:14px;font-weight:700;margin-top:8px;">Paid: ₹${b.total}</div>
        <div style="display:flex;gap:8px;margin-top:10px;">
          <button style="flex:1;padding:6px;background:#f1f5f9;border:1px solid #cbd5e1;border-radius:6px;font-size:12px;cursor:pointer;" onclick="downloadTicket('${b.pnr}')">Download E-Ticket</button>
          ${b.status === 'CANCELLED' ? '' : `<button style="padding:6px 12px;background:#fee2e2;color:#991b1b;border:1px solid #fecaca;border-radius:6px;font-size:12px;cursor:pointer;" onclick="cancelBooking('${b.pnr}')">Cancel</button>`}
        </div>
      </div>
    `).join('');
  }
  document.getElementById('bookings-drawer').style.display = 'flex';
}

function closeMyBookings(e) {
  if (!e || e.target.id === 'bookings-drawer' || e.target.classList.contains('modal-close')) {
    document.getElementById('bookings-drawer').style.display = 'none';
  }
}

async function cancelBooking(pnr) {
  if (confirm(`Cancel booking ${pnr}?`)) {
    try {
      await apiRequest(`/bookings/${encodeURIComponent(pnr)}`, { method: 'DELETE' });
      currentBookings = await apiRequest('/bookings');
      await loadBuses();
      renderBuses();
      renderSeatMap();
      updateFareSummary();
      updateBookingsBadge();
      openMyBookings();
      setApiStatus(`Booking ${pnr} was cancelled and its seats were released.`, false);
    } catch (error) {
      setApiStatus(error.message, true);
    }
  }
}

function downloadTicket(pnr) {
  const b = currentBookings.find(x => x.pnr === pnr);
  if (!b) return;
  const content = `=================================================
                GOBUS BUS TICKET E-RECEIPT
=================================================
PNR Number       : ${b.pnr}
Bus Operator     : ${b.operator}
Route            : ${b.route}
Date of Journey  : ${b.date}
Departure Time   : ${b.time}
-------------------------------------------------
Passenger Name   : ${b.passenger}
Booked Seats     : ${b.seats.join(', ')}
Total Paid Fare  : ₹${b.total}
Status           : CONFIRMED
=================================================
  Thank you for traveling with GoBus! Safe Trip!
=================================================`;

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `ticket_${b.pnr}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}
