let _toastTimer;
function showToast(msg) {
  const el = get('toast');
  el.innerHTML = `<span>${esc(msg)}</span>`;
  el.classList.add('show');
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => el.classList.remove('show'), 2200);
}

function show(id) {
  document.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.drawer-item').forEach(el => el.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  document.querySelector(`.drawer-item[data-target="${id}"]`)?.classList.add('active');
  if (id === 'archive') loadArchive();
  if (id === 'collection') loadCollection();
  if (id === 'stats') loadStats();
  const _tabMsgs = {
    wishlist:   ['ほしいものリスト、また増えた？', 'どれが一番ほしいの？', 'チェックしに来たの？'],
    collection: ['コレクション眺めてるの？', 'お気に入りが増えてきたね！', 'いいもの持ってるじゃない'],
    archive:    ['昔ほしかったものね…', 'アーカイブを掘り返してるの？', 'また気になってきた？'],
    brands:     ['ブランド整理してるの？マメね', 'お気に入りブランドはどこ？'],
    categories: ['カテゴリの管理ね', 'きちんと分類してるのね'],
    stats:      ['お財布の状況チェック？', '買いすぎてない…？', 'ちゃんと管理してるのね'],
  };
  const _tm = _tabMsgs[id];
  if (_tm && window.mascotSay) window.mascotSay(_tm[Math.floor(Math.random() * _tm.length)]);
}
function navTo(id, btn) {
  show(id);
  closeDrawer();
}
function toggleDrawer() {
  const open = document.getElementById('drawer').classList.toggle('open');
  document.getElementById('drawer-backdrop').classList.toggle('open', open);
  document.getElementById('menu-btn').classList.toggle('open', open);
}
function closeDrawer() {
  document.getElementById('drawer').classList.remove('open');
  document.getElementById('drawer-backdrop').classList.remove('open');
  document.getElementById('menu-btn').classList.remove('open');
}
function openAddModal() {
  get('add-modal').style.display = 'block';
  setTimeout(() => get('w-url').focus(), 50);
}
function closeAddModal() {
  get('add-modal').style.display = 'none';
}

const get = id => document.getElementById(id);

function showConfirm(msg, onOk) {
  get('confirm-msg').textContent = msg;
  const modal = get('confirm-modal');
  modal.style.display = 'flex';
  const ok  = get('confirm-ok');
  const cancel = get('confirm-cancel');
  const close = () => { modal.style.display = 'none'; ok.onclick = null; cancel.onclick = null; };
  ok.onclick     = () => { close(); onOk(); };
  cancel.onclick = () => close();
}
const esc = s => String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');

async function api(path, opt) {
  const r = await fetch('/api' + path, opt);
  return r.ok ? r : Promise.reject(r.status);
}

// ---- Budget ----
let _monthBudgets = JSON.parse(localStorage.getItem('gloli_month_budgets') || '{}');

const DEFAULT_MONTH_BUDGET = 30000;
function getMonthBudget(month) {
  return _monthBudgets[month] || DEFAULT_MONTH_BUDGET;
}

function editMonthBudget(evt, month, el) {
  evt.stopPropagation();
  const input = document.createElement('input');
  input.type = 'number';
  input.value = _monthBudgets[month] ?? '';
  input.className = 'month-budget-input';
  input.min = '0';
  input.step = '1000';
  input.placeholder = String(DEFAULT_MONTH_BUDGET);
  const save = () => {
    const val = parseInt(input.value) || 0;
    if (val) _monthBudgets[month] = val;
    else delete _monthBudgets[month];
    localStorage.setItem('gloli_month_budgets', JSON.stringify(_monthBudgets));
    loadStats();
  };
  input.onblur = save;
  input.onkeydown = e => { if (e.key === 'Enter') input.blur(); if (e.key === 'Escape') loadStats(); };
  el.replaceWith(input);
  input.focus(); input.select();
}


// ---- Wishlist ----
let _items = [], _sortField = null, _sortAsc = true;
let _statsMonthItems = {};
let _filter = { q: '', brandId: '', categoryId: '', status: '', priority: '' };
const PRIORITY_ORDER = { LOW: 1, MEDIUM: 2, HIGH: 3, GRAIL: 4 };

function applyFilter() {
  _filter.q          = get('f-search').value;
  _filter.brandId    = get('f-brand').value;
  _filter.categoryId = get('f-category').value;
  _filter.status     = get('f-status').value;
  _filter.priority   = get('f-priority').value;
  const n = [_filter.brandId, _filter.categoryId, _filter.status, _filter.priority].filter(Boolean).length;
  const badge = get('f-count');
  badge.textContent = n || '';
  badge.style.display = n ? '' : 'none';
  get('f-toggle-btn').classList.toggle('has-filter', n > 0);
  renderItems();
}

function clearFilter() {
  get('f-search').value = '';
  get('f-brand').value = get('f-category').value = get('f-status').value = get('f-priority').value = '';
  _filter = { q: '', brandId: '', categoryId: '', status: '', priority: '' };
  get('f-count').textContent = '';
  get('f-count').style.display = 'none';
  get('f-toggle-btn').classList.remove('has-filter');
  renderItems();
}

function toggleFilterPanel() {
  const open = get('f-panel').classList.toggle('open');
  get('f-toggle-btn').classList.toggle('open', open);
}

function renderSummary(items, totalOverride) {
  const src = items ?? _items;
  const bar = get('summary-bar');
  if (!_items.length) { bar.innerHTML = '<span>No items</span>'; return; }
  const count = src.length;
  const countLabel = totalOverride != null
    ? `<span class="s-total">${count} / ${totalOverride} items</span>`
    : `<span class="s-total">${count} items</span>`;
  const ordered = src.filter(i => i.status === 'ORDERED').length;
  const priced = src.filter(i => i.price != null);
  const totalPrice = priced.reduce((s, i) => s + Number(i.price), 0);
  const counts = { LOW: 0, MEDIUM: 0, HIGH: 0, GRAIL: 0 };
  src.forEach(i => { if (counts[i.priority] != null) counts[i.priority]++; });
  const chips = [
    counts.LOW    ? `<span class="s-chip low">Low ${counts.LOW}</span>` : '',
    counts.MEDIUM ? `<span class="s-chip medium">Medium ${counts.MEDIUM}</span>` : '',
    counts.HIGH   ? `<span class="s-chip high">High ${counts.HIGH}</span>` : '',
    counts.GRAIL  ? `<span class="s-chip grail">Grail ${counts.GRAIL}</span>` : '',
    ordered       ? `<span class="s-chip" style="border-color:#6a5010;color:#c8a040">Ordered ${ordered}</span>` : '',
  ].join('');
  bar.innerHTML = countLabel
    + (priced.length ? `<span class="s-price">¥${totalPrice.toLocaleString()}</span>` : '')
    + chips;
}

function renderItems() {
  // --- filter ---
  const q = _filter.q.toLowerCase().trim();
  let items = _items.filter(i => {
    if (q && ![ i.name, i.brand?.name, i.category?.name, i.notes, i.url ]
              .some(v => (v || '').toLowerCase().includes(q))) return false;
    if (_filter.brandId    && String(i.brand?.id    ?? '') !== _filter.brandId)    return false;
    if (_filter.categoryId && String(i.category?.id ?? '') !== _filter.categoryId) return false;
    if (_filter.status     && i.status   !== _filter.status)   return false;
    if (_filter.priority   && i.priority !== _filter.priority) return false;
    return true;
  });
  const isFiltered = q || _filter.brandId || _filter.categoryId || _filter.status || _filter.priority;
  get('f-clear').style.display = isFiltered ? '' : 'none';

  // --- sort ---
  if (_sortField) {
    items.sort((a, b) => {
      let va, vb, r;
      if (_sortField === 'price') {
        va = a.price ?? -1; vb = b.price ?? -1; r = va - vb;
      } else if (_sortField === 'priority') {
        va = PRIORITY_ORDER[a.priority] ?? 0; vb = PRIORITY_ORDER[b.priority] ?? 0; r = va - vb;
      } else {
        const get2 = (i) => (_sortField === 'brand' ? i.brand?.name : _sortField === 'category' ? i.category?.name : i.name) ?? '';
        r = get2(a).localeCompare(get2(b), 'ja');
      }
      return _sortAsc ? r : -r;
    });
  }
  renderSummary(isFiltered ? items : null, isFiltered ? _items.length : null);
  get('w-empty').style.display = items.length ? 'none' : '';
  get('w-list').innerHTML = items.length ? items.map((i, idx) => `<tr class="card-anim${i.priority === 'GRAIL' ? ' grail' : ''}" style="animation-delay:${idx*0.12}s">
    <td class="col-img" data-label=""${i.imageUrl ? ` onclick="viewImg('${esc(i.imageUrl)}')"` : ''}>${i.imageUrl ? `<img src="${esc(i.imageUrl)}" alt="image">` : '<div class="img-placeholder"></div>'}</td>
    ${(() => { try { const h = new URL(i.url).hostname; return `<td data-label="URL" title="${esc(i.url)}"><span class="url-value"><img class="favicon" src="https://www.google.com/s2/favicons?domain=${h}&sz=16" onerror="this.style.display='none'" alt=""><a href="${esc(i.url)}" target="_blank">${esc(h)}</a></span></td>`; } catch(e) { return `<td data-label="URL" title="${esc(i.url)}"><span class="url-value"><a href="${esc(i.url)}" target="_blank">${esc(i.url)}</a></span></td>`; } })()}
    <td data-label="Name" title="${esc(i.name)}"><span class="v">${esc(i.name)}</span></td>
    <td data-label="Brand" title="${esc(i.brand?.name)}"><span class="v">${esc(i.brand?.name)}</span></td>
    <td data-label="Category" title="${esc(i.category?.name)}"><span class="v">${esc(i.category?.name)}</span></td>
    <td class="col-price" data-label="Price">${i.price != null ? '¥'+Number(i.price).toLocaleString() : ''}</td>
    <td class="col-priority" data-label="Priority"><select class="priority-sel ${(i.priority||'MEDIUM').toLowerCase()}" onchange="updatePriority(${i.id},this)">${priorityOptions(i.priority)}</select></td>
    <td data-label="Notes" title="${esc(i.notes)}"><span class="v">${esc(i.notes)}</span></td>
    <td data-label="Status"><select class="status-sel ${(i.status||'WANTED').toLowerCase()}" onchange="updateStatus(${i.id},this)">${statusOptions(i.status)}</select></td>
    <td class="col-actions" data-label=""><button class="edit" onclick="openEdit(${i.id})">Edit</button> <button class="del" onclick="archiveItem(${i.id})">Archive</button></td>
  </tr>`).join('') : '<tr><td colspan="9">No items</td></tr>';
}

function sortBy(field) {
  if (_sortField === field) { _sortAsc = !_sortAsc; }
  else { _sortField = field; _sortAsc = true; }
  document.querySelectorAll('.sort-bar button[data-field]').forEach(btn => {
    btn.className = btn.dataset.field === field ? (_sortAsc ? 'active' : 'active desc') : '';
  });
  renderItems();
}

function setCols(n, btn, tbodySel) {
  document.querySelector(tbodySel).style.gridTemplateColumns =
    n ? `repeat(${n}, 1fr)` : 'repeat(auto-fill, minmax(240px, 1fr))';
  btn.closest('.sort-bar').querySelectorAll('.col-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
}

async function loadItems() {
  _items = await (await api('/wishlist')).json();
  renderItems();
}

async function addItem() {
  const url = get('w-url').value.trim();
  if (!url) return alert('URL is required.');
  if (_items.some(i => i.url === url)) { showToast('This URL is already in your list'); return; }
  const body = { url, priority: get('w-priority').value };
  const name = get('w-name').value.trim();
  const brandId = get('w-brand').value;
  const categoryId = get('w-category').value;
  const price = get('w-price').value;
  const notes = get('w-notes').value.trim();
  const imgUrl = get('w-image-url').value.trim();
  const plannedAt = get('w-planned-at').value;
  if (name) body.name = name;
  if (brandId) body.brandId = +brandId;
  if (categoryId) body.categoryId = +categoryId;
  if (price) body.price = +price;
  if (notes) body.notes = notes;
  if (imgUrl) body.imageUrl = imgUrl;
  if (plannedAt) body.plannedAt = plannedAt;
  await api('/wishlist', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });
  get('w-url').value = get('w-name').value = get('w-price').value = get('w-notes').value = get('w-image-url').value = get('w-planned-at').value = '';
  get('w-brand').value = get('w-category').value = '';
  showToast('Item added.');
  closeAddModal();
  loadItems();
  if (window.mascotSay) {
    const _n = name.length > 10 ? name.slice(0, 10) + '…' : name;
    if (body.priority === 'GRAIL') {
      window.mascotSay(_n ? `「${_n}」がグレイル！？本気なの…` : 'グレイルを登録したの…本命ね');
    } else {
      const _ms = ['また増やしたの！', '物欲が止まらないのね…', 'ほしいものリストが育ってる…', 'どんどん増えてくじゃない'];
      window.mascotSay(_ms[Math.floor(Math.random() * _ms.length)]);
    }
  }
}

function statusOptions(current) {
  return ['WANTED','ORDERED','OWNED'].map(s =>
    `<option value="${s}" ${current===s?'selected':''}>${s[0]+s.slice(1).toLowerCase()}</option>`
  ).join('');
}

function priorityOptions(current) {
  return ['LOW','MEDIUM','HIGH','GRAIL'].map(p =>
    `<option value="${p}" ${current===p?'selected':''}>${p[0]+p.slice(1).toLowerCase()}</option>`
  ).join('');
}

async function updatePriority(id, sel) {
  const priority = sel.value;
  sel.className = 'priority-sel ' + priority.toLowerCase();
  const tr = sel.closest('tr');
  if (tr) tr.className = 'card-anim' + (priority === 'GRAIL' ? ' grail' : '');
  await api('/wishlist/'+id+'/priority?priority='+priority, {method:'PATCH'});
  const item = _items.find(i => i.id === id);
  if (item) { item.priority = priority; renderSummary(); }
  if (window.mascotSay) {
    if (priority === 'GRAIL') window.mascotSay('グレイルに格上げ！？本気なの…');
    else { const _ms = ['優先度変えたの', '整理してるのね', 'ちゃんと考えてるのね']; window.mascotSay(_ms[Math.floor(Math.random() * _ms.length)]); }
  }
}

async function updateStatus(id, sel) {
  const status = sel.value;
  sel.className = 'status-sel ' + status.toLowerCase();
  await api('/wishlist/'+id+'/status?status='+status, {method:'PATCH'});
  if (status === 'OWNED') {
    showToast('Moved to Collection.');
    if (window.mascotSay) window.mascotSay('やった！ゲットしたのね！おめでとう！');
    loadItems();
  } else if (status === 'ORDERED' && window.mascotSay) {
    const _ms = ['注文したの！楽しみね！', 'もうすぐ届くね！わくわく！', '早く届くといいね！'];
    window.mascotSay(_ms[Math.floor(Math.random() * _ms.length)]);
  }
}

async function loadCollection() {
  const items = await (await api('/wishlist/owned')).json();
  const bar = get('col-summary-bar');
  if (items.length) {
    const total = items.length;
    const priced = items.filter(i => i.price != null);
    const totalPrice = priced.reduce((s, i) => s + Number(i.price), 0);
    bar.innerHTML = `<span style="color:var(--text);font-weight:600">${total} items</span>`
      + (priced.length ? `&ensp;<span style="color:#60b878">¥${totalPrice.toLocaleString()}</span>` : '');
  } else {
    bar.innerHTML = '';
  }
  get('col-empty').style.display = items.length ? 'none' : '';
  get('col-list').innerHTML = items.length ? items.map((i, idx) => `<tr class="card-anim${i.priority === 'GRAIL' ? ' grail' : ''}" style="animation-delay:${idx*0.12}s">
    <td class="col-img"${i.imageUrl ? ` onclick="viewImg('${esc(i.imageUrl)}')"` : ''}>${i.imageUrl ? `<img src="${esc(i.imageUrl)}" alt="image">` : '<div class="img-placeholder"></div>'}</td>
    <td data-label="Name"><span class="v"><a href="${esc(i.url)}" target="_blank">${esc(i.name || i.url)}</a></span></td>
    <td data-label="Brand"><span class="v">${esc(i.brand?.name)}</span></td>
    <td data-label="Category"><span class="v">${esc(i.category?.name)}</span></td>
    <td data-label="Price">${i.price != null ? '¥'+Number(i.price).toLocaleString() : ''}</td>
    <td data-label="Purchased" style="font-size:0.82rem;color:var(--muted)">${i.purchasedAt ?? ''}</td>
    <td data-label="Notes"><span class="v">${esc(i.notes)}</span></td>
    <td class="col-actions"><button class="edit" onclick="openEdit(${i.id},'collection')">Edit</button> <button class="edit" onclick="moveToWanted(${i.id})">↩ Wanted</button></td>
  </tr>`).join('') : '<tr><td colspan="8" style="color:var(--muted)">No items in collection yet</td></tr>';
}

async function moveToWanted(id) {
  await api('/wishlist/'+id+'/status?status=WANTED', {method:'PATCH'});
  showToast('Moved back to Items.');
  loadCollection(); loadItems();
}

async function archiveItem(id) {
  await api('/wishlist/'+id, {method:'DELETE'});
  showToast('Item archived.');
  if (window.mascotSay) {
    const _ms = ['諦めたの…？', 'また気が変わったら戻せるわよ', 'ま、縁がなかったのね…'];
    window.mascotSay(_ms[Math.floor(Math.random() * _ms.length)]);
  }
  loadItems();
}

async function restoreItem(id) {
  await api('/wishlist/'+id+'/restore', {method:'POST'});
  showToast('Item restored.');
  loadItems(); loadArchive();
}

async function delItemPermanent(id) {
  showConfirm('Permanently delete this item? This cannot be undone.', async () => {
    await api('/wishlist/'+id+'/permanent', {method:'DELETE'});
    showToast('Item deleted.');
    loadArchive();
  });
}

async function loadArchive() {
  const items = await (await api('/wishlist/deleted')).json();
  get('arc-empty').style.display = items.length ? 'none' : '';
  get('arc-list').innerHTML = items.length ? items.map((i, idx) => `<tr class="card-anim${i.priority === 'GRAIL' ? ' grail' : ''}" style="animation-delay:${idx*0.12}s">
    <td class="col-img" data-label=""${i.imageUrl ? ` onclick="viewImg('${esc(i.imageUrl)}')"` : ''}>${i.imageUrl ? `<img src="${esc(i.imageUrl)}" alt="image">` : '<div class="img-placeholder"></div>'}</td>
    <td data-label="Name"><span class="v"><a href="${esc(i.url)}" target="_blank">${esc(i.name || i.url)}</a></span></td>
    <td data-label="Brand"><span class="v">${esc(i.brand?.name)}</span></td>
    <td data-label="Price">${i.price != null ? '¥'+Number(i.price).toLocaleString() : ''}</td>
    <td data-label="Archived" style="font-size:0.82rem;color:var(--muted)">${i.deletedAt ? i.deletedAt.slice(0,10) : ''}</td>
    <td class="col-actions" data-label=""><button class="edit" onclick="openEdit(${i.id},'archive')">Edit</button> <button class="edit" onclick="restoreItem(${i.id})">Restore</button> <button class="del" onclick="delItemPermanent(${i.id})">Delete</button></td>
  </tr>`).join('') : '<tr><td colspan="6">No archived items</td></tr>';
}

async function loadStats() {
  const [ownedItems, wishlistItems] = await Promise.all([
    api('/wishlist/owned').then(r => r.json()),
    api('/wishlist').then(r => r.json()),
  ]);
  // ORDERED で purchasedAt が設定されているものも購入済みとして扱う
  const orderedPurchased = wishlistItems.filter(i => i.status === 'ORDERED' && i.purchasedAt);
  const purchasedItems = [...ownedItems, ...orderedPurchased];
  const hasData = purchasedItems.some(i => i.purchasedAt);
  get('stats-empty').style.display = (hasData || wishlistItems.length) ? 'none' : '';
  if (!hasData && !wishlistItems.length) {
    get('stats-hero').innerHTML = '';
    get('stats-summary').innerHTML = '';
    get('stats-monthly').innerHTML = '';
    get('stats-planned').innerHTML = '';
    return;
  }

  // --- data prep ---
  const spent = purchasedItems.filter(i => i.price != null).reduce((s, i) => s + Number(i.price), 0);
  const thisMonth = new Date().toISOString().slice(0, 7);
  const thisMonthItems = purchasedItems.filter(i => i.purchasedAt?.startsWith(thisMonth));
  const thisMonthHasPrice = thisMonthItems.some(i => i.price != null);
  const thisMonthTotal = thisMonthItems.filter(i => i.price != null).reduce((s, i) => s + Number(i.price), 0);
  const thisMonthBudget = getMonthBudget(thisMonth);

  // last month
  const lastMonthDate = new Date(); lastMonthDate.setMonth(lastMonthDate.getMonth() - 1);
  const lastMonth = lastMonthDate.toISOString().slice(0, 7);
  const lastMonthItems = purchasedItems.filter(i => i.purchasedAt?.startsWith(lastMonth));
  const lastMonthHasPrice = lastMonthItems.some(i => i.price != null);
  const lastMonthTotal = lastMonthItems.filter(i => i.price != null).reduce((s, i) => s + Number(i.price), 0);

  // planned items
  const plannedItems = wishlistItems.filter(i => i.status === 'WANTED' && i.price != null && i.plannedAt != null);
  const plannedTotal = plannedItems.reduce((s, i) => s + Number(i.price), 0);

  // --- hero: this month ---
  const heroMax = Math.max(thisMonthTotal, thisMonthBudget || 0, 1);
  const heroBarPct = Math.min(Math.round(thisMonthTotal / heroMax * 100), 100);
  const heroBudgetPct = thisMonthBudget ? Math.round(thisMonthBudget / heroMax * 100) : null;
  const heroOver = thisMonthBudget && thisMonthTotal > thisMonthBudget;
  const budgetDiff = thisMonthBudget - thisMonthTotal;
  const [ty, tm] = thisMonth.split('-');
  const budgetBadge = thisMonthBudget
    ? (budgetDiff >= 0
        ? `<span class="diff-badge under">¥${budgetDiff.toLocaleString()} remaining</span>`
        : `<span class="diff-badge over">▲ ¥${Math.abs(budgetDiff).toLocaleString()} over</span>`)
    : '';

  // vs last month
  let vsHtml = '';
  if (thisMonthHasPrice || lastMonthHasPrice) {
    const diff = thisMonthTotal - lastMonthTotal;
    const arrow = diff > 0 ? '↑' : diff < 0 ? '↓' : '→';
    const cls   = diff > 0 ? 'vs-up' : diff < 0 ? 'vs-down' : 'vs-neutral';
    const diffTxt = diff !== 0 ? `¥${Math.abs(diff).toLocaleString()}` : '変化なし';
    vsHtml = `<div class="stats-hero-vs">
      vs 先月
      <span class="${cls}">${arrow} ${diffTxt}</span>
      <span class="stats-hero-vs-counts">${lastMonthItems.length} → ${thisMonthItems.length} items</span>
    </div>`;
  }

  // upcoming forecast (WANTED items with plannedAt, future months only)
  const forecastMap = {};
  for (const i of wishlistItems.filter(i => i.status === 'WANTED' && i.plannedAt)) {
    const m = i.plannedAt.slice(0, 7);
    if (m >= thisMonth) {
      if (!forecastMap[m]) forecastMap[m] = { count: 0, total: 0, hasPrice: false };
      forecastMap[m].count++;
      if (i.price != null) { forecastMap[m].total += Number(i.price); forecastMap[m].hasPrice = true; }
    }
  }
  const forecastKeys = Object.keys(forecastMap).sort().slice(0, 4);
  const forecastHtml = forecastKeys.length ? `
    <div class="stats-hero-forecast">
      <div class="stats-hero-forecast-label">Upcoming</div>
      <div class="stats-hero-forecast-chips">
        ${forecastKeys.map(k => {
          const [fy, fm] = k.split('-');
          const fd = forecastMap[k];
          const isCurrent = k === thisMonth;
          return `<div class="stats-forecast-chip${isCurrent ? ' current' : ''}">
            <div class="fc-month">${fy}/${parseInt(fm)}</div>
            <div class="fc-amount">${fd.hasPrice ? '¥'+fd.total.toLocaleString() : '—'}</div>
            <div class="fc-count">${fd.count} items</div>
          </div>`;
        }).join('')}
      </div>
    </div>` : '';

  get('stats-hero').innerHTML = `<div class="stats-hero">
    <div class="stats-hero-top">
      <span class="stats-hero-month">${ty}/${parseInt(tm)}</span>
      <span class="stats-hero-item-count">${thisMonthItems.length} items</span>
    </div>
    <div class="stats-hero-amount${heroOver ? ' over' : ''}">${thisMonthHasPrice ? '¥'+thisMonthTotal.toLocaleString() : '—'}</div>
    ${thisMonthBudget ? `
      <div class="stats-hero-bar-wrap">
        ${heroBudgetPct != null ? `<div class="stats-hero-budget-marker" style="left:${heroBudgetPct}%"></div>` : ''}
        <div class="stats-hero-bar${heroOver ? ' over' : ''}" style="width:${heroBarPct}%"></div>
      </div>
      <div class="stats-hero-budget-label">budget ¥${thisMonthBudget.toLocaleString()}</div>` : ''}
    <div class="stats-hero-meta">${budgetBadge}</div>
    ${vsHtml}
    ${forecastHtml}
  </div>`;

  // --- summary cards (simplified) ---
  const plannedCardHtml = plannedItems.length ? `
    <div class="stat-card">
      <div class="stat-val">¥${plannedTotal.toLocaleString()}</div>
      <div class="stat-label">Planned spending</div>
    </div>` : '';

  get('stats-summary').innerHTML = `
    <div class="stat-card"><div class="stat-val">${purchasedItems.length}</div><div class="stat-label">Total purchased</div></div>
    <div class="stat-card"><div class="stat-val">${purchasedItems.filter(i=>i.price!=null).length ? '¥'+spent.toLocaleString() : '—'}</div><div class="stat-label">Total spent</div></div>
    ${plannedCardHtml}`;

  // --- monthly table ---
  if (!hasData) { get('stats-hero').innerHTML = ''; get('stats-monthly').innerHTML = ''; get('stats-planned').innerHTML = ''; return; }
  const map = {};
  _statsMonthItems = {};
  for (const i of purchasedItems) {
    if (!i.purchasedAt) continue;
    const key = i.purchasedAt.slice(0, 7);
    if (!map[key]) map[key] = { count: 0, total: 0, hasPrice: false };
    map[key].count++;
    if (i.price != null) { map[key].total += Number(i.price); map[key].hasPrice = true; }
    if (!_statsMonthItems[key]) _statsMonthItems[key] = [];
    _statsMonthItems[key].push(i);
  }
  const keys = Object.keys(map).sort().reverse();
  const allRowBudgets = keys.map(k => getMonthBudget(k) || 0);
  const maxBar = Math.max(...Object.values(map).map(d => d.total), ...allRowBudgets, 1);
  const monthBlocks = keys.map(k => {
    const [y, m] = k.split('-');
    const d = map[k];
    const rowBudget = getMonthBudget(k);
    const pct = d.hasPrice ? Math.round(d.total / maxBar * 100) : 0;
    const over = rowBudget && d.hasPrice && d.total > rowBudget;
    const rowBudgetPct = rowBudget ? Math.round(rowBudget / maxBar * 100) : null;
    const isOverridden = _monthBudgets[k] != null;
    let diffHtml = '';
    if (rowBudget && d.hasPrice) {
      const diff = rowBudget - d.total;
      diffHtml = diff >= 0
        ? `<span class="diff-badge under">+¥${diff.toLocaleString()}</span>`
        : `<span class="diff-badge over">-¥${Math.abs(diff).toLocaleString()}</span>`;
    }
    const budgetChip = `<span class="month-budget-chip${rowBudget ? (isOverridden ? ' overridden' : '') : ' empty'}" onclick="editMonthBudget(event,'${k}',this)">${rowBudget ? '¥'+rowBudget.toLocaleString() : '¥—'}</span>`;
    const items = _statsMonthItems[k] || [];
    const itemRows = items.map(i => {
      const pClass = (i.priority || 'medium').toLowerCase();
      const pLabel = i.priority ? i.priority[0] + i.priority.slice(1).toLowerCase() : 'Medium';
      return `<div class="plan-item${i.priority === 'GRAIL' ? ' grail' : ''}" data-url="${esc(i.url)}" onclick="const u=this.dataset.url;if(u)window.open(u,'_blank')">
        ${i.imageUrl ? `<img src="${esc(i.imageUrl)}" class="plan-thumb" alt="">` : '<div class="plan-thumb plan-thumb-empty"></div>'}
        <div class="plan-item-info">
          <span class="plan-item-name">${esc(i.name || i.url)}</span>
          ${i.brand ? `<span class="plan-item-brand">${esc(i.brand.name)}</span>` : ''}
        </div>
        <div class="plan-item-right">
          ${i.price != null ? `<span class="plan-item-price">¥${Number(i.price).toLocaleString()}</span>` : ''}
          <span class="plan-priority ${pClass}">${pLabel}</span>
        </div>
      </div>`;
    }).join('');
    return `<div class="plan-month-block${over ? ' plan-over' : ''}">
      <div class="plan-month-header">
        <span class="plan-month-label">${y}/${parseInt(m)}</span>
        <span class="plan-month-count">${d.count} items</span>
        ${budgetChip}
        ${d.hasPrice ? `<span class="plan-month-total">¥${d.total.toLocaleString()}</span>` : ''}
        ${diffHtml}
      </div>
      ${d.hasPrice ? `<div class="month-bar-wrap">
        ${rowBudgetPct != null ? `<div class="stat-budget-line" style="left:${rowBudgetPct}%"></div>` : ''}
        <div class="stat-bar${over ? ' over' : ''}" style="width:${pct}%"></div>
      </div>` : ''}
      <div class="plan-items">${itemRows}</div>
    </div>`;
  }).join('');
  get('stats-monthly').innerHTML = `
    <h3 class="stats-section-title">Monthly Spending</h3>
    <div class="plan-timeline">${monthBlocks}</div>`;

  // --- planned purchases timeline ---
  const plannedWanted = wishlistItems.filter(i => i.status === 'WANTED' && i.plannedAt);
  const planMap = {};
  for (const i of plannedWanted) {
    const key = i.plannedAt.slice(0, 7);
    if (!planMap[key]) planMap[key] = { items: [], total: 0, hasPrice: false };
    planMap[key].items.push(i);
    if (i.price != null) { planMap[key].total += Number(i.price); planMap[key].hasPrice = true; }
  }
  const planKeys = Object.keys(planMap).sort();
  if (!planKeys.length) { get('stats-planned').innerHTML = ''; return; }

  const planBlocks = planKeys.map(k => {
    const [y, m] = k.split('-');
    const d = planMap[k];
    const isPast = k < thisMonth;
    const isCurrent = k === thisMonth;
    const badge = isPast
      ? `<span class="plan-badge overdue">Overdue</span>`
      : isCurrent ? `<span class="plan-badge current">This month</span>` : '';
    const planMonthBudget = getMonthBudget(k);
    const isPlanOver = planMonthBudget && d.hasPrice && d.total > planMonthBudget;
    let diffHtml = '';
    if (planMonthBudget && d.hasPrice) {
      const diff = planMonthBudget - d.total;
      diffHtml = diff >= 0
        ? `<span class="diff-badge under">+¥${diff.toLocaleString()}</span>`
        : `<span class="diff-badge over">-¥${Math.abs(diff).toLocaleString()}</span>`;
    }
    const planBudgetIsOverridden = _monthBudgets[k] != null;
    const planBudgetChip = `<span class="month-budget-chip plan-budget-chip${planMonthBudget ? (planBudgetIsOverridden ? ' overridden' : '') : ' empty'}" onclick="editMonthBudget(event,'${k}',this)">${planMonthBudget ? '¥'+planMonthBudget.toLocaleString() : '¥—'}</span>`;
    const itemRows = d.items.map(i => {
      const pClass = (i.priority || 'medium').toLowerCase();
      const pLabel = i.priority ? i.priority[0] + i.priority.slice(1).toLowerCase() : 'Medium';
      return `<div class="plan-item${i.priority === 'GRAIL' ? ' grail' : ''}" data-url="${esc(i.url)}" onclick="const u=this.dataset.url;if(u)window.open(u,'_blank')">
        ${i.imageUrl ? `<img src="${esc(i.imageUrl)}" class="plan-thumb" alt="">` : '<div class="plan-thumb plan-thumb-empty"></div>'}
        <div class="plan-item-info">
          <span class="plan-item-name">${esc(i.name || i.url)}</span>
          ${i.brand ? `<span class="plan-item-brand">${esc(i.brand.name)}</span>` : ''}
        </div>
        <div class="plan-item-right">
          ${i.price != null ? `<span class="plan-item-price">¥${Number(i.price).toLocaleString()}</span>` : ''}
          <span class="plan-priority ${pClass}">${pLabel}</span>
        </div>
      </div>`;
    }).join('');
    return `<div class="plan-month-block${isPast ? ' plan-past' : isCurrent ? ' plan-current' : (isPlanOver ? ' plan-over' : '')}">
      <div class="plan-month-header">
        <span class="plan-month-label">${y}/${parseInt(m)}${badge}</span>
        <span class="plan-month-count">${d.items.length} items</span>
        ${planBudgetChip}
        ${d.hasPrice ? `<span class="plan-month-total">¥${d.total.toLocaleString()}</span>` : ''}
        ${diffHtml}
      </div>
      <div class="plan-items">${itemRows}</div>
    </div>`;
  }).join('');
  get('stats-planned').innerHTML = `
    <h3 class="stats-section-title" style="margin-top:28px">Planned Purchases</h3>
    <div class="plan-timeline">${planBlocks}</div>`;
}

let _editItem = null, _editContext = 'wishlist';
async function openEdit(id, context) {
  _editContext = context || 'wishlist';
  _editItem = await (await api('/wishlist/'+id)).json();
  const i = _editItem;
  get('edit-id').value = i.id;
  get('edit-url').value = i.url ?? '';
  get('edit-name').value = i.name ?? '';
  get('edit-price').value = i.price ?? '';
  get('edit-planned-at').value = i.plannedAt ?? '';
  get('edit-purchased-at').value = i.purchasedAt ?? '';
  get('edit-notes').value = i.notes ?? '';
  // sync select options then restore value
  const bSel = get('edit-brand');
  bSel.innerHTML = get('w-brand').innerHTML;
  bSel.value = i.brand?.id ?? '';
  const cSel = get('edit-category');
  cSel.innerHTML = get('w-category').innerHTML;
  cSel.value = i.category?.id ?? '';
  // image preview
  get('edit-image-file').value = '';
  get('edit-file-name').textContent = 'No file chosen';
  const isExternal = i.imageUrl && !i.imageUrl.startsWith('/api/');
  get('edit-image-url').value = isExternal ? i.imageUrl : '';
  const preview = get('edit-image-preview');
  if (i.imageUrl) { preview.src = i.imageUrl + (isExternal ? '' : '?t=' + Date.now()); preview.style.display = 'block'; }
  else { preview.style.display = 'none'; preview.src = ''; }
  get('edit-modal').style.display = 'block';  // modal-overlay uses display:block
}

function previewImgFile(input) {
  const preview = get('edit-image-preview');
  if (input.files && input.files[0]) {
    get('edit-image-url').value = '';
    get('edit-file-name').textContent = input.files[0].name;
    preview.src = URL.createObjectURL(input.files[0]);
    preview.style.display = 'block';
  }
}

function previewImgUrl(input) {
  const preview = get('edit-image-preview');
  const url = input.value.trim();
  if (url) {
    get('edit-image-file').value = '';
    get('edit-file-name').textContent = 'No file chosen';
    preview.src = url;
    preview.style.display = 'block';
  } else {
    preview.style.display = 'none';
    preview.src = '';
  }
}

function viewImg(url) {
  get('img-modal-src').src = url + '?t=' + Date.now();
  get('img-modal').style.display = 'block';
}

function closeEdit() {
  get('edit-modal').style.display = 'none';
}

async function saveItem() {
  const url = get('edit-url').value.trim();
  if (!url) return alert('URL is required.');
  const id = get('edit-id').value;
  const body = { url, priority: _editItem?.priority ?? 'MEDIUM', status: _editItem?.status ?? 'WANTED' };
  const name = get('edit-name').value.trim();
  const brandId = get('edit-brand').value;
  const categoryId = get('edit-category').value;
  const price = get('edit-price').value;
  const notes = get('edit-notes').value.trim();
  const plannedAt   = get('edit-planned-at').value;
  const purchasedAt = get('edit-purchased-at').value;
  if (name) body.name = name;
  if (brandId) body.brandId = +brandId;
  if (categoryId) body.categoryId = +categoryId;
  if (price) body.price = +price;
  if (notes) body.notes = notes;
  if (plannedAt)   body.plannedAt   = plannedAt;
  if (purchasedAt) body.purchasedAt = purchasedAt;
  const imgFile = get('edit-image-file').files[0];
  const imgUrl = get('edit-image-url').value.trim();
  if (!imgFile && imgUrl) body.imageUrl = imgUrl;
  await api('/wishlist/'+id, { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });
  if (imgFile) {
    const fd = new FormData();
    fd.append('file', imgFile);
    await api('/wishlist/'+id+'/image', { method:'POST', body:fd });
  }
  closeEdit();
  showToast('Item saved.');
  if (_editContext === 'collection') loadCollection();
  else if (_editContext === 'archive') loadArchive();
  else loadItems();
  if (window.mascotSay) {
    const _ms = ['ちゃんと更新したのね', '保存完了よ', '直したの？えらいえらい'];
    window.mascotSay(_ms[Math.floor(Math.random() * _ms.length)]);
  }
}

// ---- Brands ----
async function loadBrands() {
  const brands = await (await api('/brands')).json();
  ['w-brand', 'edit-brand'].forEach(sid => {
    const sel = get(sid);
    if (!sel) return;
    const cur = sel.value;
    sel.innerHTML = '<option value="">Brand</option>' + brands.map(b => `<option value="${b.id}">${esc(b.name)}</option>`).join('');
    sel.value = cur;
  });
  const fb = get('f-brand'), fbCur = fb.value;
  fb.innerHTML = '<option value="">All brands</option>' + brands.map(b => `<option value="${b.id}">${esc(b.name)}</option>`).join('');
  fb.value = fbCur;
  get('b-list').innerHTML = brands.map(b => `<tr>
    <td>${esc(b.name)}</td>
    <td>${b.url ? `<a href="${esc(b.url)}" target="_blank">${esc(b.url)}</a>` : ''}</td>
    <td><button class="edit" onclick="editBrand(${b.id},'${esc(b.name)}','${esc(b.url??'')}')">Edit</button> <button class="del" onclick="delBrand(${b.id})">Delete</button></td>
  </tr>`).join('') || '<tr><td colspan="3">No brands</td></tr>';
}

async function addBrand() {
  const name = get('b-name').value.trim();
  if (!name) return alert('Brand name is required.');
  const url = get('b-url').value.trim();
  await api('/brands', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name, url:url||null}) })
    .catch(s => s===409 ? alert('Already exists.') : null);
  get('b-name').value = get('b-url').value = '';
  loadBrands();
}

async function delBrand(id) {
  showConfirm('Delete this brand?', async () => { await api('/brands/'+id, {method:'DELETE'}); loadBrands(); });
}

function editBrand(id, name, url) {
  const row = [...document.querySelectorAll('#b-list tr')].find(r => r.innerHTML.includes(`editBrand(${id},`));
  if (!row) return;
  row.classList.add('editing');
  row.innerHTML = `
    <td><input id="eb-name-${id}" value="${esc(name)}" placeholder="Brand name (required)"></td>
    <td><input id="eb-url-${id}" value="${esc(url)}" placeholder="Official URL" size="30"></td>
    <td>
      <button class="save" onclick="saveBrand(${id})">Save</button>
      <button onclick="loadBrands()">Cancel</button>
    </td>`;
}

async function saveBrand(id) {
  const name = get('eb-name-'+id).value.trim();
  if (!name) return alert('Brand name is required.');
  const url = get('eb-url-'+id).value.trim();
  await api('/brands/'+id, { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name, url:url||null}) })
    .catch(s => s===409 ? alert('Already exists.') : null);
  loadBrands();
}

// ---- Inline brand creation ----
function openInlineBrand(selId) {
  const wrap = get(selId + '-new');
  wrap.classList.add('open');
  const inp = get(selId === 'w-brand' ? 'w-brand-input' : 'edit-brand-input');
  inp.value = '';
  inp.focus();
  inp.onkeydown = e => { if (e.key === 'Enter') confirmInlineBrand(selId); if (e.key === 'Escape') closeInlineBrand(selId); };
}
function closeInlineBrand(selId) {
  get(selId + '-new').classList.remove('open');
}
async function confirmInlineBrand(selId) {
  const inp = get(selId === 'w-brand' ? 'w-brand-input' : 'edit-brand-input');
  const name = inp.value.trim();
  if (!name) return;
  try {
    const res = await api('/brands', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name}) });
    const created = await res.json();
    await loadBrands();
    get(selId).value = created.id;
    closeInlineBrand(selId);
    showToast(`Brand "${esc(name)}" added`);
  } catch(s) {
    if (s === 409) showToast('This brand already exists');
  }
}

// ---- Inline category creation ----
function openInlineCat(selId) {
  const wrap = get(selId + '-new');
  wrap.classList.add('open');
  const inp = get(selId === 'w-category' ? 'w-cat-input' : 'edit-cat-input');
  inp.value = '';
  inp.focus();
  inp.onkeydown = e => { if (e.key === 'Enter') confirmInlineCat(selId); if (e.key === 'Escape') closeInlineCat(selId); };
}
function closeInlineCat(selId) {
  get(selId + '-new').classList.remove('open');
}
async function confirmInlineCat(selId) {
  const inp = get(selId === 'w-category' ? 'w-cat-input' : 'edit-cat-input');
  const name = inp.value.trim();
  if (!name) return;
  try {
    const res = await api('/categories', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name}) });
    const created = await res.json();
    await loadCategories();
    get(selId).value = created.id;
    closeInlineCat(selId);
    showToast(`Category "${esc(name)}" added`);
  } catch(s) {
    if (s === 409) showToast('This category already exists');
  }
}

// ---- Categories ----
async function loadCategories() {
  const cats = await (await api('/categories')).json();
  ['w-category', 'edit-category'].forEach(sid => {
    const sel = get(sid);
    if (!sel) return;
    const cur = sel.value;
    sel.innerHTML = '<option value="">Category</option>' + cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
    sel.value = cur;
  });
  const fc = get('f-category'), fcCur = fc.value;
  fc.innerHTML = '<option value="">All categories</option>' + cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  fc.value = fcCur;
  get('c-list').innerHTML = cats.map(c => `<tr>
    <td>${esc(c.name)}</td>
    <td><button class="edit" onclick="editCategory(${c.id},'${esc(c.name)}')">Edit</button> <button class="del" onclick="delCategory(${c.id})">Delete</button></td>
  </tr>`).join('') || '<tr><td colspan="2">No categories</td></tr>';
}

async function addCategory() {
  const name = get('c-name').value.trim();
  if (!name) return alert('Category name is required.');
  await api('/categories', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name}) })
    .catch(s => s===409 ? alert('Already exists.') : null);
  get('c-name').value = '';
  loadCategories();
}

async function delCategory(id) {
  showConfirm('Delete this category?', async () => { await api('/categories/'+id, {method:'DELETE'}); loadCategories(); });
}

function editCategory(id, name) {
  const row = [...document.querySelectorAll('#c-list tr')].find(r => r.innerHTML.includes(`editCategory(${id},`));
  if (!row) return;
  row.classList.add('editing');
  row.innerHTML = `
    <td><input id="ec-name-${id}" value="${esc(name)}" placeholder="Category name (required)"></td>
    <td>
      <button class="save" onclick="saveCategory(${id})">Save</button>
      <button onclick="loadCategories()">Cancel</button>
    </td>`;
}

async function saveCategory(id) {
  const name = get('ec-name-'+id).value.trim();
  if (!name) return alert('Category name is required.');
  await api('/categories/'+id, { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name}) })
    .catch(s => s===409 ? alert('Already exists.') : null);
  loadCategories();
}

async function fetchInfo(btn, prefix) {
  const url = get(prefix + '-url').value.trim();
  if (!url) return;
  const orig = btn.textContent;
  btn.textContent = '···';
  btn.disabled = true;
  try {
    const info = await (await api('/scrape?url=' + encodeURIComponent(url))).json();
    if (info.name)     get(prefix + '-name').value  = info.name;
    if (info.price)    get(prefix + '-price').value = info.price;
    if (info.imageUrl) {
      get(prefix + '-image-url').value = info.imageUrl;
      if (prefix === 'edit') previewImgUrl(get('edit-image-url'));
    }
    if (info.brandId) {
      const sel = get(prefix + '-brand');
      const opt = [...sel.options].find(o => Number(o.value) === info.brandId);
      if (opt) sel.value = opt.value;
    } else if (info.brand) {
      const sel = get(prefix + '-brand');
      const opt = [...sel.options].find(o => o.text.toLowerCase() === info.brand.toLowerCase());
      if (opt) sel.value = opt.value;
    }
    if (info.category) {
      const sel = get(prefix + '-category');
      const q = info.category.toLowerCase();
      const opt = [...sel.options].find(o => o.text.toLowerCase() === q)
               ?? [...sel.options].find(o => o.text.toLowerCase().includes(q) || q.includes(o.text.toLowerCase()));
      if (opt) sel.value = opt.value;
    }
    btn.textContent = 'Done';
    setTimeout(() => { btn.textContent = orig; btn.disabled = false; }, 1000);
  } catch {
    btn.textContent = 'Failed';
    setTimeout(() => { btn.textContent = orig; btn.disabled = false; }, 1500);
  }
}

// ---- Loading messages ----
const _loadMsgs = ['Just a moment…', 'Fetching data…', 'Almost there…'];
let _lmi = 0;
const _lb = document.getElementById('loading-bubble');
const _lmTimer = setInterval(() => { _lb.textContent = _loadMsgs[++_lmi % _loadMsgs.length]; }, 1000);

function _dismissLoading() {
  clearInterval(_lmTimer);
  const ls = document.getElementById('loading-screen');
  if (!ls) return;
  ls.classList.add('fade-out');
  setTimeout(() => {
    if (ls.parentNode) ls.remove();
    document.querySelectorAll('.card-anim').forEach((el, idx) => {
      el.style.animation = 'none';
      void el.offsetWidth;
      el.style.animation = '';
      el.style.animationDelay = (idx * 0.07) + 's';
    });
    setTimeout(() => {
      if (window.mascotSay) {
        const _gs = ['いらっしゃい！今日もチェックしに来たの？', 'おかえり！ほしいものは増えてる？', 'こんにちは！何か気になるものある？', 'また来たの？うれしい！'];
        window.mascotSay(_gs[Math.floor(Math.random() * _gs.length)]);
      }
    }, 400);
  }, 500);
}
// Hard fallback: force-dismiss after 8 seconds no matter what
setTimeout(_dismissLoading, 8000);
Promise.all([loadItems(), loadBrands(), loadCategories()])
  .then(_dismissLoading)
  .catch(err => { _dismissLoading(); showToast('Failed to load data.'); console.error('Initial load failed:', err); });

// ---- Mascot ----
(function() {
  const el = document.getElementById('mascot-wrap');
  const _bubble = document.getElementById('mascot-bubble');
  let _bubbleTimer = null;

  const _msgs = [
    '何を買おうか悩んでるの？',
    'またウィッシュリストが増えてる…',
    'grailはもう手に入れた？',
    '今日もお買い物？',
    '気になるなら、すぐ追加したほうがいいわよ',
    'ちゃんと予算は大丈夫？',
  ];

  function _trunc(n, max) {
    return n && n.length > max ? n.slice(0, max) + '…' : (n || '???');
  }

  function _pickMsg() {
    if (!_items.length) return _msgs[Math.floor(Math.random() * _msgs.length)];
    const MAX = 10;
    const dyn = [];
    dyn.push(`${_items.length}個も登録してるの…`);
    const priced = _items.filter(i => i.price != null);
    if (priced.length) {
      const total = priced.reduce((s, i) => s + Number(i.price), 0);
      dyn.push(`合計¥${total.toLocaleString()}…大丈夫？`);
      const top = priced.reduce((a, b) => Number(a.price) >= Number(b.price) ? a : b);
      if (top.name) dyn.push(`¥${Number(top.price).toLocaleString()}は高いわね…`);
    }
    const grails = _items.filter(i => i.priority === 'GRAIL');
    if (grails.length) {
      const g = grails[Math.floor(Math.random() * grails.length)];
      if (g.name) dyn.push(`「${_trunc(g.name, MAX)}」がグレイルなの？`);
    }
    const ordered = _items.filter(i => i.status === 'ORDERED');
    if (ordered.length) dyn.push(`${ordered.length}個注文中ね。楽しみ！`);
    const named = _items.filter(i => i.name);
    if (named.length) {
      const r = named[Math.floor(Math.random() * named.length)];
      dyn.push(`「${_trunc(r.name, MAX)}」はもう手に入れた？`);
    }
    const pool = [..._msgs, ...dyn];
    return pool[Math.floor(Math.random() * pool.length)];
  }

  window.mascotSay = function(msg, duration) {
    _bubble.textContent = msg;
    _bubble.style.opacity = '1';
    clearTimeout(_bubbleTimer);
    _bubbleTimer = setTimeout(() => { _bubble.style.opacity = ''; }, duration || 4000);
  };

  function showBubble() { window.mascotSay(_pickMsg()); }

  const _img = el.querySelector('img');
  _img.addEventListener('mouseenter', () => {
    _img.classList.remove('mascot-bounce');
    void _img.offsetWidth;
    _img.classList.add('mascot-bounce');
  });
  _img.addEventListener('animationend', () => _img.classList.remove('mascot-bounce'));

  function scheduleSpeak() {
    const delay = 8000 + Math.random() * 12000;
    setTimeout(() => { showBubble(); scheduleSpeak(); }, delay);
  }
  scheduleSpeak();

  // Drag
  let ox = 0, oy = 0, active = false, _dragMoved = false, _startX = 0, _startY = 0;

  function applyPos(x, y) {
    x = Math.max(0, Math.min(x, window.innerWidth  - el.offsetWidth));
    y = Math.max(0, Math.min(y, window.innerHeight - el.offsetHeight));
    el.style.left = x + 'px'; el.style.top = y + 'px';
    el.style.right = 'auto'; el.style.bottom = 'auto';
  }
  function savePos() {
    localStorage.setItem('mascot-pos-v3', JSON.stringify({ left: el.offsetLeft, top: el.offsetTop }));
  }

  const _saved = JSON.parse(localStorage.getItem('mascot-pos-v3') || 'null');
  if (_saved) applyPos(_saved.left, _saved.top);

  _img.addEventListener('mousedown', e => {
    active = true; _dragMoved = false;
    _startX = e.clientX; _startY = e.clientY;
    const r = el.getBoundingClientRect();
    ox = e.clientX - r.left; oy = e.clientY - r.top;
    e.preventDefault();
  });
  document.addEventListener('mousemove', e => {
    if (!active) return;
    if (!_dragMoved && Math.abs(e.clientX - _startX) + Math.abs(e.clientY - _startY) > 5) _dragMoved = true;
    applyPos(e.clientX - ox, e.clientY - oy);
  });
  document.addEventListener('mouseup', () => {
    if (active) { active = false; if (!_dragMoved) showBubble(); else savePos(); }
  });

  let _touchMoved = false;
  _img.addEventListener('touchstart', e => {
    active = true; _touchMoved = false;
    const t = e.touches[0], r = el.getBoundingClientRect();
    ox = t.clientX - r.left; oy = t.clientY - r.top;
    e.preventDefault();
  }, { passive: false });
  document.addEventListener('touchmove', e => {
    if (!active) return;
    _touchMoved = true;
    applyPos(e.touches[0].clientX - ox, e.touches[0].clientY - oy);
    e.preventDefault();
  }, { passive: false });
  document.addEventListener('touchend', () => {
    if (active) { active = false; if (!_touchMoved) showBubble(); else savePos(); }
  });
})();

// ---- Refresh All ----
async function refreshAllItems() {
  const btn = get('refresh-all-btn');
  btn.disabled = true;
  btn.classList.add('spinning');
  try {
    const result = await (await api('/wishlist/refresh-all', { method: 'POST' })).json();
    const msg = result.failed > 0
      ? `Updated ${result.updated} / ${result.total} items (${result.failed} failed)`
      : `Updated ${result.updated} / ${result.total} items`;
    showToast(msg);
    loadItems();
    if (window.mascotSay) {
      if (result.failed > 0) window.mascotSay('いくつか取得できなかったの…');
      else window.mascotSay('全部チェックしたよ！価格が変わってないといいね…');
    }
  } catch {
    showToast('Refresh failed.');
  } finally {
    btn.disabled = false;
    btn.classList.remove('spinning');
  }
}

// ---- Refresh ----
async function refreshPage() {
  const btn = get('refresh-btn');
  btn.classList.add('spinning');
  btn.disabled = true;
  const active = document.querySelector('.tab.active')?.id || 'wishlist';
  const tasks = [loadItems(), loadBrands(), loadCategories()];
  if (active === 'collection') tasks.push(loadCollection());
  if (active === 'archive')    tasks.push(loadArchive());
  if (active === 'stats')      tasks.push(loadStats());
  await Promise.all(tasks);
  btn.classList.remove('spinning');
  btn.disabled = false;
  showToast('Updated.');
}

// ---- Scroll to top ----
(function() {
  const btn = document.getElementById('scroll-top');
  window.addEventListener('scroll', () => btn.classList.toggle('visible', window.scrollY > 220), { passive: true });
  btn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
})();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');
}
