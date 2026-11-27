import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

// Launch a headless Chromium/Edge with a dedicated profile/devtools port first.
const mode = 'api';
const frontend = process.env.MEAL_SMOKE_ORIGIN ?? 'http://localhost:5173';
const apiOrigin = process.env.MEAL_SMOKE_API_ORIGIN ?? frontend;
const debuggerOrigin = process.env.MEAL_SMOKE_DEBUGGER ?? 'http://127.0.0.1:9223';
const output = process.env.MEAL_SMOKE_OUTPUT ?? '../.local/browser-smoke';
const layoutOnly = process.argv.includes('--layout-only');
const pricesAndFriday = process.argv.includes('--prices-friday');
const calendarRates = process.argv.includes('--calendar-rates');
const desktopWidths = [1440, 1366, 1280];
const mobileWidths = [320, 375, 430];
const employee = process.env.MEAL_SMOKE_EMPLOYEE;
const admin = process.env.MEAL_SMOKE_ADMIN;
const password = process.env.MEAL_SMOKE_PASSWORD;
const adminPassword = process.env.MEAL_SMOKE_ADMIN_PASSWORD ?? password;
assert.ok(employee && admin && password, 'Supply test usernames and passwords for an isolated database.');

const newTarget = await fetch(`${debuggerOrigin}/json/new?about:blank`, { method: 'PUT' });
assert.ok(newTarget.ok, 'Could not create an isolated browser tab.');
const target = await newTarget.json();
const socket = new WebSocket(target.webSocketDebuggerUrl);
const pending = new Map();
const requests = new Map();
const responses = [];
const assetErrors = [];
const exceptions = [];
let sequence = 0;
socket.addEventListener('message', event => {
  const message = JSON.parse(event.data);
  if (message.id) {
    const call = pending.get(message.id);
    if (!call) return;
    pending.delete(message.id);
    if (message.error) call.reject(Error(message.error.message));
    else call.resolve(message.result);
  } else if (message.method === 'Network.requestWillBeSent') {
    requests.set(message.params.requestId, message.params.request.method);
  } else if (message.method === 'Network.responseReceived') {
    const { requestId, response } = message.params;
    const { pathname, origin } = new URL(response.url);
    if (pathname.startsWith('/api/')) responses.push({ pathname, origin, method: requests.get(requestId), status: response.status });
    else if (response.status >= 400) assetErrors.push({ pathname, status: response.status });
  } else if (message.method === 'Runtime.exceptionThrown') {
    const detail = message.params.exceptionDetails;
    exceptions.push(detail.exception?.description ?? detail.text);
  } else if (message.method === 'Page.javascriptDialogOpening') {
    send('Page.handleJavaScriptDialog', { accept: true }).catch(() => {});
  }
});
await new Promise((resolve, reject) => {
  socket.addEventListener('open', resolve, { once: true });
  socket.addEventListener('error', reject, { once: true });
});

function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++sequence;
    pending.set(id, { resolve, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw Error(result.exceptionDetails.exception?.description ?? result.exceptionDetails.text);
  return result.result.value;
}
async function until(expression, description) {
  for (let attempt = 0; attempt < 160; attempt++) {
    if (await evaluate(expression)) return;
    await delay(125);
  }
  const location = await evaluate('location.pathname');
  throw Error(`Timeout: ${description}. Route: ${location}. API statuses: ${JSON.stringify(responses.slice(-8))}. Asset errors: ${JSON.stringify(assetErrors.slice(-8))}`);
}
async function visit(route, title) {
  await send('Page.navigate', { url: frontend + route });
  await until(`location.pathname === ${JSON.stringify(route.split('?')[0])} && document.body?.innerText.includes(${JSON.stringify(title)})`, `${route} shows ${title}`);
}
async function fill(selector, value) {
  await evaluate(`(() => {
    const field = document.querySelector(${JSON.stringify(selector)});
    const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(field), 'value').set;
    setter.call(field, ${JSON.stringify(String(value))});
    field.dispatchEvent(new Event('input', { bubbles: true }));
    field.dispatchEvent(new Event('change', { bubbles: true }));
  })()`);
}
async function clickText(selector, text) {
  const clicked = await evaluate(`(() => {
    const button = [...document.querySelectorAll(${JSON.stringify(selector)})].find(item => item.textContent.trim().includes(${JSON.stringify(text)}));
    if (!button || button.disabled) return false;
    button.click();
    return true;
  })()`);
  assert.ok(clicked, `Could not click ${text}`);
}
async function signIn(username) {
  await visit('/dang-nhap', 'Đăng nhập');
  await until("!!document.querySelector('#username') && !!document.querySelector('#password')", 'sign-in form is mounted');
  await fill('#username', username);
  await fill('#password', username === admin ? adminPassword : password);
  await clickText('button[type=submit]', 'Đăng nhập');
  await until("location.pathname === '/dang-ky' && !!document.querySelector('.month-calendar')", 'sign-in opens registration');
}
async function signOut() {
  await evaluate("document.querySelector('.account-menu__trigger').click()");
  await clickText('.account-menu__dropdown button', 'Đăng xuất');
  await until("location.pathname === '/dang-nhap'", 'sign-out opens login');
}
async function viewport(width, height) {
  await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
  await until(`innerWidth === ${width}`, `viewport updated to ${width}px`);
}
async function capture(name) {
  const { data } = await send('Page.captureScreenshot', { format: 'png' });
  await writeFile(path.join(output, `${mode}-${name}.png`), Buffer.from(data, 'base64'));
}

async function openCompactNavigation(selector) {
  await evaluate(`document.querySelector('${selector} .compact-navigation__trigger').click()`);
  assert.ok(await evaluate(`(() => {
    const list = document.querySelector('${selector} .compact-navigation__items');
    const buttons = list.querySelectorAll('button');
    return list.dataset.expanded === 'true' && getComputedStyle(list).overflowY === 'auto'
      && buttons[0].offsetTop < buttons[1].offsetTop && list.scrollWidth <= list.clientWidth + 1;
  })()`), `${selector} opens as a vertically scrollable list`);
}

async function verifyRegistrationLayout() {
  await until("!document.querySelector('.registration-panel__loading')", 'registration loaded');
  for (const width of desktopWidths) {
    await viewport(width, 900);
    const layout = await evaluate(`(() => {
      const bounds = selector => document.querySelector(selector).getBoundingClientRect();
      return {
        tabs: getComputedStyle(document.querySelector('.registration-mobile-tabs')).display,
        roster: bounds('.registration-department-area').width,
        panel: bounds('.registration-panel').width,
        centered: Math.abs(bounds('.registration-panel').left + 406 - document.documentElement.clientWidth / 2) < 2,
        summaryOnRight: bounds('.month-calendar').right <= bounds('.registration-summary').left,
        fits: document.documentElement.scrollWidth <= innerWidth + 1,
      };
    })()`);
    assert.equal(layout.tabs, 'grid', `fork desktop tabs at ${width}px`);
    assert.ok(layout.roster === 0 && layout.panel === 812 && layout.centered && layout.summaryOnRight && layout.fits,
      `centered fork layout at ${width}px: ${JSON.stringify(layout)}`);
    await capture(`desktop-${width}`);
  }
  await clickText('.registration-mobile-tabs button', 'Tra cứu phòng');
  await until("!!document.querySelector('.registration-lookup-card__table-wrap')?.offsetWidth", 'desktop roster tab loads');
  await clickText('.registration-mobile-tabs button', 'Đăng ký cơm');
  for (const width of mobileWidths) {
    await viewport(width, 844);
    assert.equal(await evaluate("getComputedStyle(document.querySelector('.registration-mobile-tabs')).display"), 'none');
    await openCompactNavigation('.registration-navigation');
    await capture(`mobile-menu-${width}`);
    await clickText('.registration-mobile-tabs button', 'Tra cứu phòng');
    await until("document.querySelector('.registration-department-area').classList.contains('is-visible')", 'roster opens from mobile menu');
    assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth + 1'), `page fits at ${width}px`);
    await openCompactNavigation('.registration-navigation');
    await clickText('.registration-mobile-tabs button', 'Đăng ký cơm');
  }
  await evaluate("document.querySelector('.account-menu__trigger').click()");
  assert.ok(await evaluate("!!document.querySelector('.account-menu__icon').offsetWidth && getComputedStyle(document.querySelector('.account-menu__dropdown')).overflowY === 'auto'"));
  assert.equal(await evaluate("[...document.querySelectorAll('.account-menu__dropdown a')].filter(link => link.pathname === '/thuc-don').length"), 0);
  await evaluate("document.dispatchEvent(new KeyboardEvent('keydown', {key:'Escape'}))");
  await viewport(1366, 900);
}

async function verifyMenuPopup() {
  await clickText('.registration-summary__actions button', 'Xem thực đơn');
  await until("!!document.querySelector('.preview-modal img') || document.querySelector('.preview-modal')?.innerText.includes('Chưa có')", 'menu resolves to image or empty state');
  await clickText('.preview-modal footer button', 'Đóng');
  await send('Page.navigate', { url: frontend + '/thuc-don' });
  await until("location.pathname === '/dang-ky' && !!document.querySelector('.preview-modal')", 'old menu URL opens the registration popup');
  await clickText('.preview-modal footer button', 'Đóng');
  await until("!document.querySelector('.preview-modal') && !location.search.includes('menu=')", 'popup closes in place');
}

async function verifyCalendarRates() {
  const total = '.registration-summary__details div:last-child dd';
  async function expectTotal(value) {
    await until(`document.querySelector('${total}')?.textContent === '${value} VNĐ'`, `calendar total is ${value} VND`);
  }
  await expectTotal('25.000');
  for (const width of desktopWidths) {
    await viewport(width, 900);
    assert.ok(await evaluate(`(() => {
      const panel = document.querySelector('.registration-panel').getBoundingClientRect();
      const note = document.querySelector('.friday-special-panel');
      const box = note.getBoundingClientRect();
      return box.left > panel.right && box.right <= document.documentElement.clientWidth
        && getComputedStyle(note).position === 'sticky';
    })()`), `sticky note fits at ${width}px`);
  }
  await clickText('.friday-special-panel__buttons button', 'Chọn suất thường');
  await expectTotal('50.000');
  await clickText('.friday-special-panel__buttons button', 'Chọn suất đặc biệt');
  await expectTotal('65.000');
  await viewport(1440, 900);
  await capture('calendar-rate-note');
  for (const width of mobileWidths) {
    await viewport(width, 844);
    assert.ok(await evaluate(`(() => {
      const note = document.querySelector('.friday-special-panel').getBoundingClientRect();
      const submit = document.querySelector('.registration-panel__footer button').getBoundingClientRect();
      return note.left >= 0 && note.right <= innerWidth && note.bottom < submit.top;
    })()`), `note fits above save at ${width}px`);
  }
  await clickText('.registration-panel__footer button', 'Lưu đăng ký');
  await until("!document.querySelector('.registration-panel__unsaved')", 'calendar changes are saved');
  await visit('/dang-ky', 'Hệ thống đăng ký cơm trưa');
  await expectTotal('65.000');
  assert.ok(responses.some(r => r.pathname === '/api/meal-registrations' && r.method === 'POST' && r.status === 200));
}

async function verifyDepartmentColumns(selector) {
  const headers = await evaluate(`Array.from(document.querySelectorAll(${JSON.stringify(selector)}), table =>
    Array.from(table.querySelectorAll('th'), heading => Math.round(heading.getBoundingClientRect().left)))`);
  assert.ok(headers.length > 1, 'more than one department is visible');
  assert.ok(headers.every(row => row.join(',') === headers[0].join(',')), `${selector}: columns align across departments`);
}

async function verifyAdminPeople() {
  await clickText('.admin-nav button', 'Nhân viên & phòng ban');
  await until("!!document.querySelector('.admin-table--employees tbody tr')", 'people table has employees');
  await verifyDepartmentColumns('.admin-table--employees');
  await capture('admin-people');
  await clickText('.admin-table button', 'Chi tiết');
  await until("!!document.querySelector('.admin-employee-modal')", 'employee detail opens');
  await evaluate("document.querySelector('.admin-employee-modal button[aria-label=Đóng]').click()");
}

async function verifyPeopleLayout() {
  await signOut();
  await signIn(admin);
  await visit('/admin', 'Tổng quan');
  await verifyAdminPeople();
  await viewport(375, 844);
  assert.ok(await evaluate(`(() => {
    const wrap = document.querySelector('.admin-table--employees').parentElement;
    return wrap.scrollWidth > wrap.clientWidth && getComputedStyle(wrap).overflowX === 'auto'
      && document.documentElement.scrollWidth <= innerWidth;
  })()`), 'on mobile only the table region scrolls horizontally');
  await openCompactNavigation('.admin-navigation');
  await capture('admin-mobile-nav');
  await clickText('.admin-nav button', 'Đăng ký & giao dịch');
  await until("!!document.querySelector('.admin-ticket-navigation')", 'admin ticket navigation opens');
  await evaluate("document.querySelector('.admin-registration-filters input[type=checkbox]').click()");
  await until("document.querySelectorAll('.admin-table--roster').length > 1", 'department registration tables load');
  await verifyDepartmentColumns('.admin-table--roster');
  await openCompactNavigation('.admin-ticket-navigation');
  await capture('admin-mobile-ticket-nav');
  await clickText('.preview-subnav button', 'Lịch sử trao đổi');
  await until("document.body.innerText.includes('Nhật ký chợ vé')", 'ticket submenu opens transfer history');
  for (const endpoint of ['/api/auth/login', '/api/meal-registrations/month', '/api/departments/roster', '/api/admin/employees']) {
    assert.ok(responses.some(entry => entry.pathname === endpoint && entry.status === 200), `${endpoint} loaded via API`);
  }
  assert.ok(responses.every(response => response.origin === apiOrigin), 'API requests use the configured origin');
}

async function verifyPricesAndFriday() {
  await signOut();
  await signIn(admin);
  await visit('/admin', 'Tổng quan');
  await clickText('.admin-nav button', 'Thực đơn & giá vé');
  const table = '.admin-price-table';
  const normal = `${table} tbody tr:first-child`;
  const special = `${table} tbody tr:nth-child(2)`;
  await until(`document.querySelector('${normal} td:nth-child(2) strong')?.innerText.includes('25.000')
    && document.querySelector('${special} td:nth-child(2) strong')?.innerText.includes('40.000')`, 'standard prices are effective');
  for (const [label, row, price] of [['vé thường', normal, 26000], ['vé thường', normal, 25000], ['vé đặc biệt', special, 40000]]) {
    await fill(`input[aria-label="Giá mới ${label}"]`, price);
    await clickText(`${row} button`, 'Lưu giá');
    await until(`document.querySelector('${row} td:nth-child(2) strong')?.innerText.includes(${JSON.stringify(price.toLocaleString('vi-VN'))})
      && !document.querySelector('${row} button').disabled`, 'admin price save and reload');
  }
  if (process.env.MEAL_SMOKE_MENU_FILE) {
    await send('DOM.enable');
    const { root } = await send('DOM.getDocument');
    const { nodeId } = await send('DOM.querySelector', { nodeId: root.nodeId, selector: '.admin-menu-card input[type=file]' });
    await send('DOM.setFileInputFiles', { nodeId, files: [process.env.MEAL_SMOKE_MENU_FILE] });
    await until("!!document.querySelector('.admin-menu-actions button:last-child:enabled')", 'admin can save the selected image');
    await clickText('.admin-menu-actions button', 'Lưu ảnh');
    await until("!!document.querySelector('.admin-menu-preview') && document.querySelector('.admin-menu-actions button:last-child').disabled", 'admin saves the image');
  }
  await signOut();
  await signIn(employee);
  const panel = '.friday-special-panel';
  await until("!document.querySelector('.registration-panel__loading')", 'registration data loaded');
  const canChoose = `!!document.querySelector('${panel} .friday-special-panel__buttons button:last-child:enabled')`;
  const movedToNextMonth = !await evaluate(canChoose);
  if (movedToNextMonth) {
    await evaluate("document.querySelector('.month-calendar__navigation button:last-child').click()");
  }
  await until(canChoose, 'Friday selector is visible and editable');
  assert.match(await evaluate(`document.querySelector('${panel}').innerText`), /40\.000/);
  async function saveSelection() {
    if (await evaluate("!!document.querySelector('.registration-panel__unsaved')")) {
      await clickText('.registration-panel__footer button', 'Lưu đăng ký');
      await until("!document.querySelector('.registration-panel__unsaved') && !document.querySelector('.registration-panel__footer button').disabled", 'registration save completes');
    }
  }
  assert.ok(await evaluate(`(() => {
    const normal = document.querySelector('${panel} .friday-special-panel__buttons button:first-child:enabled');
    if (!normal) return false;
    normal.click();
    return true;
  })()`), 'normal meal can be chosen');
  await saveSelection();
  await clickText(`${panel} button`, 'Chọn suất đặc biệt');
  await saveSelection();
  await visit('/dang-ky', 'Hệ thống đăng ký cơm trưa');
  await until("!document.querySelector('.registration-panel__loading')", 'saved calendar reloads');
  if (movedToNextMonth) {
    await evaluate("document.querySelector('.month-calendar__navigation button:last-child').click()");
  }
  await until(`document.querySelector('${panel}')?.innerText.includes('Đã chọn đặc biệt')`, 'saved special meal loads again');
  await capture('friday-special');
  if (process.env.MEAL_SMOKE_MENU_FILE) {
    await clickText('.registration-summary__actions button', 'Xem thực đơn');
    await until("document.querySelector('.preview-modal img')?.naturalWidth === 1", 'employee loads uploaded PNG');
    await clickText('.preview-modal footer button', 'Đóng');
    assert.ok(responses.some(r => r.pathname === '/api/admin/menus' && r.method === 'PUT' && r.status === 200));
  }
  assert.ok(responses.filter(r => r.pathname === '/api/admin/meal-prices' && r.method === 'POST' && r.status === 201).length >= 3);
  assert.ok(responses.some(r => r.pathname === '/api/meal-registrations' && r.method === 'POST' && r.status === 200));
}

async function verifyRegistrationWrite() {
  const findFree = `[...document.querySelectorAll('.month-calendar__date')].findIndex(
    button => !button.disabled && button.getAttribute('aria-pressed') === 'false')`;
  let index = await evaluate(findFree);
  if (index === -1) {
    await evaluate("document.querySelector('.month-calendar__navigation button:last-child').click()");
    await until(`(${findFree}) >= 0`, 'next month has a day available to register');
    index = await evaluate(findFree);
  }
  const dayButton = `document.querySelectorAll('.month-calendar__date')[${index}]`;
  for (const selected of [true, false]) {
    await evaluate(`${dayButton}.click()`);
    await clickText('.registration-panel__footer button', 'Lưu đăng ký');
    await until(`!document.querySelector('.registration-panel__unsaved') && ${dayButton}?.getAttribute('aria-pressed') === '${selected}'`,
      selected ? 'registration was saved' : 'registration was cancelled');
  }
}

async function verifyWorkflows() {
  const selectAll = await evaluate("document.querySelector('.registration-summary__actions button')?.textContent.trim()");
  if (selectAll === 'Chọn tất cả') {
    const canSelect = await evaluate("!document.querySelector('.registration-summary__actions button').disabled");
    if (canSelect) {
      await clickText('.registration-summary__actions button', 'Chọn tất cả');
      await until("!!document.querySelector('.registration-panel__unsaved')", 'select-all shows draft changes');
      await clickText('.registration-summary__actions button', 'Bỏ chọn tất cả');
    }
  }
  await visit('/dang-ky', 'Hệ thống đăng ký cơm trưa');
  await until("!document.querySelector('.registration-panel__loading')", 'saved calendar loads');
  await verifyRegistrationWrite();
  await visit('/thong-tin-ca-nhan', 'Thông tin cá nhân');
  await until("!!document.querySelector('#full-name')?.value", 'profile data loaded');
  await clickText('.profile-form button[type=submit]', 'Lưu');
  await until("document.querySelector('.app-toast--success')?.textContent.includes('Đã cập nhật thông tin cá nhân')", 'profile save succeeds');
  await visit('/mua-them-ve', 'Mua thêm vé');
  await until("document.querySelector('.preview-purchase-quote')?.innerText.includes('25.000') && !document.querySelector('.preview-form button').disabled", 'configured price loads');
  await fill('.preview-form input[type=number]', 2);
  await until("document.querySelector('.preview-purchase-quote')?.innerText.includes('50.000')", 'quote reflects quantity');
  await fill('.preview-form input[type=number]', 1);
  await capture('purchase-price');
  const reason = `Browser smoke ${Date.now()}`;
  await fill('.preview-form textarea', reason);
  await clickText('.preview-form button', 'Gửi yêu cầu');
  await until(`document.querySelector('.preview-table')?.innerText.includes(${JSON.stringify(reason)})`, 'purchase is in employee history');
  assert.ok(await evaluate(`Array.from(document.querySelectorAll('.preview-table tbody tr')).some(row =>
    row.innerText.includes(${JSON.stringify(reason)}) && row.innerText.includes('25.000'))`), 'saved total equals preview');
  await visit('/trao-doi-ve', 'Chuyển nhượng vé');
  await until("!document.body.innerText.includes('Đang tải chợ vé') && !!document.querySelector('.preview-market-sell')", 'market data loaded');
  await signOut();
  await signIn(admin);
  await visit('/admin', 'Tổng quan');
  await verifyAdminPeople();
  await clickText('.admin-nav button', 'Thực đơn & giá vé');
  await until("!!document.querySelector('.admin-price-table tbody tr')", 'admin menu and price fields load');
  await clickText('button', 'Đăng ký & giao dịch');
  await until("Array.from(document.querySelectorAll('button')).some(b => b.textContent.includes('Duyệt mua thêm'))", 'admin ticket tabs visible');
  await clickText('button', 'Quy định');
  await until("!!document.querySelector('.admin-settings-card input[type=time]')?.value", 'registration settings load');
  await clickText('button', 'Duyệt mua thêm');
  await until(`[...document.querySelectorAll('tr')].some(row => row.innerText.includes(${JSON.stringify(reason)}))`, 'admin sees the same request');
  const approved = await evaluate(`(() => {
    const row = [...document.querySelectorAll('tr')].find(row => row.innerText.includes(${JSON.stringify(reason)}));
    const button = [...row.querySelectorAll('button')].find(item => item.textContent === 'Duyệt');
    if (!button || button.disabled) return false;
    setTimeout(() => button.click(), 0);
    return true;
  })()`);
  assert.ok(approved, 'Approve button is enabled');
  await until(`[...document.querySelectorAll('tr')].some(row => row.innerText.includes(${JSON.stringify(reason)}) && row.innerText.includes('Đã duyệt'))`, 'admin approval updates the table');
  await clickText('button', 'Lịch sử trao đổi');
  await until("document.body.innerText.includes('Nhật ký chợ vé') && !document.body.innerText.includes('Đang tải dữ liệu...')", 'admin transfer log loads');
  await clickText('button', 'Báo cáo & thống kê');
  await until("!!document.querySelector('.rv4-link')", 'report has employees and tickets');
  const hasMeals = await evaluate(`(() => {
    const bar = [...document.querySelectorAll('.preview-chart-bar-button')]
      .find(button => Number(button.querySelector('.preview-chart-value').textContent) > 0);
    if (!bar) return false;
    bar.click();
    return true;
  })()`);
  assert.ok(hasMeals, 'Report shows a day with tickets');
  await until("document.querySelector('.rv4-detail-heading').textContent.includes('Riêng ngày')", 'chart filters employee detail');
  await clickText('.rv4-link', 'Xem vé');
  await until("!!document.querySelector('.rv4-drawer tbody tr')", 'employee drawer shows individual tickets');
  await capture('report-detail');
  await evaluate("document.querySelector('.rv4-drawer button[aria-label]').click()");
  await send('Browser.setDownloadBehavior', { behavior: 'deny' });
  await clickText('.rv4-toolbar-actions button', 'Xuất tổng hợp cả kỳ');
  await until("!!document.querySelector('.app-toast--error') || document.querySelector('.app-toast--success')?.textContent.includes('Đã xuất báo cáo tổng hợp cả kỳ')", 'report export completes');
  assert.equal(await evaluate("document.querySelector('.app-toast--error')?.textContent"), undefined);
  await signOut();
  await signIn(employee);
  assert.equal(await evaluate('document.title'), 'Đặt cơm trưa');
  await visit('/mua-them-ve', 'Mua thêm vé');
  await until(`[...document.querySelectorAll('tr')].some(row => row.innerText.includes(${JSON.stringify(reason)}) && row.innerText.includes('Đã duyệt'))`, 'employee sees approval after reload');
  assert.deepEqual(exceptions, []);
  assert.ok(responses.some(r => r.pathname === '/api/ticket-purchases/quote' && r.method === 'GET' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/ticket-purchases' && r.method === 'POST' && r.status === 201));
  assert.ok(responses.some(r => r.pathname === '/api/departments/roster' && r.status === 200));
  assert.ok(responses.some(r => /^\/api\/admin\/purchases\/\d+$/.test(r.pathname) && r.method === 'PATCH' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/admin/reports/detail' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/meal-registrations' && r.method === 'POST' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/users/me' && r.method === 'PATCH' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/admin/meal-prices' && r.status === 200));
  assert.ok(responses.some(r => r.pathname === '/api/admin/reports/range/export' && r.status === 200));
  assert.ok(responses.every(r => r.origin === apiOrigin), 'API requests use the configured origin');
  await send('Network.setBlockedURLs', { urls: [apiOrigin + '/api/departments/roster*'] });
  await visit('/dang-ky', 'Hệ thống đăng ký cơm trưa');
  await clickText('.registration-mobile-tabs button', 'Tra cứu phòng');
  await until("!!document.querySelector('.registration-lookup-card [role=alert]')", 'network failure shows an error');
  assert.equal(await evaluate("!!document.querySelector('.registration-lookup-card__table-wrap')"), false);
  return reason;
}

try {
  await mkdir(output, { recursive: true });
  await Promise.all(['Page.enable', 'Runtime.enable', 'Network.enable'].map(method => send(method)));
  await send('Storage.clearDataForOrigin', { origin: frontend, storageTypes: 'local_storage' });
  await viewport(1366, 900);
  await signIn(employee);
  assert.equal(await evaluate('document.title'), 'Đặt cơm trưa');
  await verifyRegistrationLayout();
  await verifyMenuPopup();
  if (calendarRates) await verifyCalendarRates();
  else if (pricesAndFriday) await verifyPricesAndFriday();
  else if (layoutOnly) await verifyPeopleLayout();
  const purchaseMarker = layoutOnly || pricesAndFriday || calendarRates ? undefined : await verifyWorkflows();
  assert.deepEqual(exceptions, []);
  console.log(JSON.stringify({ mode, result: 'PASS', widths: [...desktopWidths, ...mobileWidths], layoutOnly, pricesAndFriday, calendarRates, purchaseMarker,
    routes: [...new Set(responses.map(entry => entry.pathname))] }));
} finally {
  await fetch(`${debuggerOrigin}/json/close/${target.id}`).catch(() => {});
  if (socket.readyState === WebSocket.OPEN) socket.close();
  await delay(100);
}
