const API = window.location.origin;
let categories = [], locations = [];
let selectedCategory = 'All', selectedLocation = 'All';
let searchTimeout;

async function api(path, opts = {}) {
  const res = await fetch(API + path, { headers: {'Content-Type':'application/json'}, ...opts });
  if (res.status === 204) return null;
  if (!res.ok) { const t = await res.text(); throw new Error(t); }
  return res.json();
}

function toast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2500);
}

// --- Navigation ---
function showPage(name, btn) {
  stopScanner();
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + name).classList.add('active');
  document.querySelectorAll('.bottom-nav button').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  if (name === 'home') loadHome();
  if (name === 'items') { loadItems(); loadCategoriesForFilter(); loadLocationsForFilter(); }
  if (name === 'add') { resetForm(); loadCategoriesForForm(); loadLocationsForForm(); }
  if (name === 'manage') { loadCategoriesList(); loadLocationsList(); }
}

function showPageDirect(name) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + name).classList.add('active');
  document.querySelectorAll('.bottom-nav button').forEach(b => b.classList.remove('active'));
  const idx = ['home','items','add','manage'].indexOf(name);
  if (idx >= 0) document.querySelectorAll('.bottom-nav button')[idx].classList.add('active');
  if (name === 'home') loadHome();
  if (name === 'items') { loadItems(); loadCategoriesForFilter(); loadLocationsForFilter(); }
}

// --- Home ---
async function loadHome() {
  document.getElementById('today-date').textContent = new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  try {
    const s = await api('/stats/');
    document.getElementById('stat-total').textContent = s.total_items;
    document.getElementById('stat-expiring').textContent = s.expiring_soon_count;
    document.getElementById('stat-expired').textContent = s.expired_count;
    document.getElementById('d-by-cat').innerHTML = Object.entries(s.by_category).sort((a,b)=>b[1]-a[1]).map(([k,v]) =>
      `<div class="dash-row"><span>${getCatEmoji(k)} ${k}</span><span class="val">${v}</span></div>`).join('') || '<div class="empty-msg"><span class="empty-icon">🛒</span>No items yet — start adding!</div>';
    document.getElementById('d-by-loc').innerHTML = Object.entries(s.by_location).sort((a,b)=>b[1]-a[1]).map(([k,v]) =>
      `<div class="dash-row"><span>${getLocEmoji(k)} ${k}</span><span class="val">${v}</span></div>`).join('') || '<div class="empty-msg">No items yet</div>';
  } catch(e) { console.error(e); }
}

function getCatEmoji(cat) {
  const m = { rice:'🍚', 'cereals & pulses':'🌾', oil:'🫒', snacks:'🍿', sugar:'🧂', sweets:'🍬', powders:'🥣', spices:'🌶️', dairy:'🥛', vegetables:'🥬', fruits:'🍎', beverages:'🥤', meat:'🍖', other:'📦' };
  return m[cat] || '📦';
}
function getLocEmoji(loc) {
  if (loc.includes('fridge')) return '❄️';
  if (loc.includes('freezer')) return '🧊';
  if (loc.includes('pantry')) return '🗄️';
  if (loc.includes('shelf')) return '📚';
  return '📍';
}

// --- Filter chips ---
async function loadCategoriesForFilter() {
  try {
    categories = await api('/categories/');
    document.getElementById('category-chips').innerHTML =
      `<div class="chip ${selectedCategory==='All'?'active':''}" onclick="filterCategory('All')">All</div>` +
      categories.map(c => `<div class="chip ${selectedCategory===c.name?'active':''}" onclick="filterCategory('${c.name.replace(/'/g,"\\'")}')">${getCatEmoji(c.name)} ${c.name}</div>`).join('');
  } catch(e) { console.error(e); }
}

async function loadLocationsForFilter() {
  try {
    locations = await api('/locations/');
    document.getElementById('location-chips').innerHTML =
      `<div class="chip ${selectedLocation==='All'?'active':''}" onclick="filterLocation('All')">All</div>` +
      locations.map(l => `<div class="chip ${selectedLocation===l.name?'active':''}" onclick="filterLocation('${l.name.replace(/'/g,"\\'")}')">${getLocEmoji(l.name)} ${l.name}</div>`).join('');
  } catch(e) { console.error(e); }
}

function filterCategory(cat) { selectedCategory = cat; loadCategoriesForFilter(); loadItems(); }
function filterLocation(loc) { selectedLocation = loc; loadLocationsForFilter(); loadItems(); }

// --- Items ---
async function loadItems() {
  try {
    const params = new URLSearchParams();
    const search = document.getElementById('search-input').value;
    if (search) params.set('search', search);
    if (selectedCategory !== 'All') params.set('category', selectedCategory);
    if (selectedLocation !== 'All') params.set('location', selectedLocation);
    const items = await api('/items/?' + params);
    document.getElementById('item-count').textContent = items.length;
    const container = document.getElementById('items-list');
    if (!items.length) { container.innerHTML = '<div class="empty-msg"><span class="empty-icon">🛒</span>No items found.<br>Tap ➕ to add your first item!</div>'; return; }
    const today = new Date().toISOString().split('T')[0];
    const soon = new Date(Date.now() + 3*86400000).toISOString().split('T')[0];
    container.innerHTML = items.map(i => {
      let expClass = '', expText = '', cardClass = '';
      if (i.expiry_date) {
        if (i.expiry_date < today) { expClass = 'expired'; expText = `⚠️ Expired: ${i.expiry_date}`; cardClass = 'expired-card'; }
        else if (i.expiry_date <= soon) { expClass = 'expiring'; expText = `⏰ Expiring: ${i.expiry_date}`; cardClass = 'expiring-card'; }
        else { expText = `📅 Expires: ${i.expiry_date}`; }
      }
      return `<div class="item-card ${cardClass}" onclick="editItem(${i.id})">
        <div class="item-info">
          <div class="item-name">${getCatEmoji(i.category)} ${i.name}</div>
          <div class="item-meta">${i.quantity} ${i.unit}</div>
          ${expText ? `<div class="item-expiry ${expClass}">${expText}</div>` : ''}
        </div>
        <div class="item-badges">
          <span class="badge badge-loc">${getLocEmoji(i.location)} ${i.location}</span>
          <span class="badge badge-cat">${i.category}</span>
        </div>
        <button class="btn-consume" onclick="event.stopPropagation();consumeItem(${i.id})" title="Use 1">🍽️</button>
        <button class="btn-del" onclick="event.stopPropagation();deleteItem(${i.id})">🗑️</button>
      </div>`;
    }).join('');
  } catch(e) { console.error(e); document.getElementById('items-list').innerHTML = '<div class="empty-msg">Error loading items</div>'; }
}

async function deleteItem(id) {
  if (!confirm('Delete this item?')) return;
  await api('/items/' + id, { method: 'DELETE' });
  toast('🗑️ Item deleted');
  loadItems();
}

async function consumeItem(id) {
  const res = await api('/items/' + id + '/consume', { method: 'POST' });
  toast(res.deleted ? '✅ Item finished & removed' : '🍽️ Used 1');
  loadItems();
}

// --- Barcode Scanner ---
let scanner = null, scannerRunning = false;

function toggleScanner() {
  if (scannerRunning) { stopScanner(); return; }
  document.getElementById('scan-btn').innerHTML = '⏹️ Stop Scanner';
  document.getElementById('scan-status').innerHTML = '<div class="scan-loading">Starting camera...</div>';
  scanner = new Html5Qrcode("scanner-container");
  scanner.start({ facingMode: "environment" }, { fps: 10, qrbox: { width: 250, height: 150 } }, onScanSuccess)
    .then(() => { scannerRunning = true; document.getElementById('scan-status').innerHTML = '<div class="scan-loading">📷 Point at a barcode...</div>'; })
    .catch(err => { document.getElementById('scan-status').innerHTML = `<div class="scan-loading">Camera error: ${err}</div>`; document.getElementById('scan-btn').innerHTML = '📷 Scan Barcode'; });
}

function stopScanner() {
  if (scanner && scannerRunning) { scanner.stop().then(() => scanner.clear()).catch(() => {}); }
  scannerRunning = false;
  document.getElementById('scan-btn').innerHTML = '📷 Scan Barcode';
  document.getElementById('scanner-container').innerHTML = '';
}

async function onScanSuccess(barcode) {
  stopScanner();
  document.getElementById('scan-status').innerHTML = `<div class="scan-loading">Looking up barcode: ${barcode}...</div>`;
  document.getElementById('edit-id').dataset.barcode = barcode;
  try {
    const data = await api('/barcode/' + barcode);
    if (data.name) {
      document.getElementById('f-name').value = data.name;
      if (data.category) { const sel = document.getElementById('f-category'); for (let i = 0; i < sel.options.length; i++) { if (sel.options[i].value === data.category) { sel.value = data.category; break; } } }
      if (data.unit) document.getElementById('f-unit').value = data.unit;
      const src = data.source === 'local' ? '✅ Found in your saved products' : '🌐 Found online';
      document.getElementById('scan-status').innerHTML = `<div class="scan-result"><div class="product-name">${data.name}</div><div>${src}</div><div>Barcode: ${barcode}</div></div>`;
    } else {
      document.getElementById('scan-status').innerHTML = `<div class="scan-result">Barcode <b>${barcode}</b> not found.<br>Enter the name — it'll auto-fill next time! 🎯</div>`;
    }
  } catch(e) {
    document.getElementById('scan-status').innerHTML = `<div class="scan-result">Barcode: <b>${barcode}</b><br>Lookup failed. Enter details manually.</div>`;
  }
}

// --- Form ---
async function loadCategoriesForForm() {
  categories = await api('/categories/');
  document.getElementById('f-category').innerHTML = categories.map(c => `<option value="${c.name}">${getCatEmoji(c.name)} ${c.name}</option>`).join('');
}
async function loadLocationsForForm() {
  locations = await api('/locations/');
  document.getElementById('f-location').innerHTML = locations.map(l => `<option value="${l.name}">${getLocEmoji(l.name)} ${l.name}</option>`).join('');
}

function resetForm() {
  stopScanner();
  document.getElementById('scan-status').innerHTML = '';
  document.getElementById('scan-btn').style.display = '';
  document.getElementById('edit-id').value = '';
  document.getElementById('edit-id').dataset.barcode = '';
  document.getElementById('form-title').innerHTML = '<span class="emoji">➕</span>Add Item';
  document.getElementById('f-name').value = '';
  document.getElementById('f-qty').value = '';
  document.getElementById('f-unit').value = 'pcs';
  document.getElementById('f-purchase').value = new Date().toISOString().split('T')[0];
  document.getElementById('f-expiry').value = '';
  document.getElementById('f-notes').value = '';
}

async function editItem(id) {
  stopScanner();
  document.getElementById('scan-status').innerHTML = '';
  document.getElementById('scan-btn').style.display = 'none';
  const item = await api('/items/' + id);
  document.getElementById('edit-id').value = id;
  document.getElementById('form-title').innerHTML = '<span class="emoji">✏️</span>Edit Item';
  document.getElementById('f-name').value = item.name;
  document.getElementById('f-qty').value = item.quantity;
  document.getElementById('f-unit').value = item.unit;
  document.getElementById('f-purchase').value = item.purchase_date;
  document.getElementById('f-expiry').value = item.expiry_date || '';
  document.getElementById('f-notes').value = item.notes || '';
  await loadCategoriesForForm();
  await loadLocationsForForm();
  document.getElementById('f-category').value = item.category;
  document.getElementById('f-location').value = item.location;
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-add').classList.add('active');
  document.querySelectorAll('.bottom-nav button').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.bottom-nav button')[2].classList.add('active');
}

async function saveItem() {
  const name = document.getElementById('f-name').value.trim();
  const qty = parseFloat(document.getElementById('f-qty').value);
  if (!name || isNaN(qty)) { alert('Name and quantity are required'); return; }
  const body = {
    name, quantity: qty,
    unit: document.getElementById('f-unit').value,
    category: document.getElementById('f-category').value,
    location: document.getElementById('f-location').value,
    purchase_date: document.getElementById('f-purchase').value,
    expiry_date: document.getElementById('f-expiry').value || null,
    notes: document.getElementById('f-notes').value || null,
    barcode: document.getElementById('edit-id').dataset.barcode || null,
  };
  const editId = document.getElementById('edit-id').value;
  if (editId) { await api('/items/' + editId, { method: 'PUT', body: JSON.stringify(body) }); toast('✅ Item updated!'); }
  else { await api('/items/', { method: 'POST', body: JSON.stringify(body) }); toast('✅ Item added!'); }
  showPageDirect('items');
}

// --- Category Management ---
async function loadCategoriesList() {
  categories = await api('/categories/');
  document.getElementById('cat-list').innerHTML = categories.map(c =>
    `<div class="manage-row"><span>${getCatEmoji(c.name)} ${c.name}</span><button class="btn-del" onclick="deleteCategory(${c.id})">🗑️</button></div>`
  ).join('') || '<div class="empty-msg">No categories yet</div>';
}
async function addCategory() {
  const input = document.getElementById('new-cat-name');
  if (!input.value.trim()) return;
  await api('/categories/', { method: 'POST', body: JSON.stringify({ name: input.value.trim() }) });
  input.value = ''; loadCategoriesList(); toast('✅ Category added!');
}
async function deleteCategory(id) {
  if (!confirm('Delete this category?')) return;
  await api('/categories/' + id, { method: 'DELETE' }); loadCategoriesList(); toast('🗑️ Category deleted');
}

// --- Location Management ---
async function loadLocationsList() {
  locations = await api('/locations/');
  document.getElementById('loc-list').innerHTML = locations.map(l =>
    `<div class="manage-row"><span>${getLocEmoji(l.name)} ${l.name}</span><button class="btn-del" onclick="deleteLocation(${l.id})">🗑️</button></div>`
  ).join('') || '<div class="empty-msg">No locations yet</div>';
}
async function addLocation() {
  const input = document.getElementById('new-loc-name');
  if (!input.value.trim()) return;
  await api('/locations/', { method: 'POST', body: JSON.stringify({ name: input.value.trim() }) });
  input.value = ''; loadLocationsList(); toast('✅ Location added!');
}
async function deleteLocation(id) {
  if (!confirm('Delete this location?')) return;
  await api('/locations/' + id, { method: 'DELETE' }); loadLocationsList(); toast('🗑️ Location deleted');
}

// --- Search ---
document.getElementById('search-input').addEventListener('input', () => {
  clearTimeout(searchTimeout); searchTimeout = setTimeout(loadItems, 300);
});

// --- Init ---
loadHome();
