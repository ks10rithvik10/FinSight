const $ = id => document.getElementById(id);
const root = $('root');
let token = localStorage.getItem('finsight_token'), me = null, txs = [], budgets = [], goals = [], charts = {};
const localeMap = { INR: 'en-IN', USD: 'en-US', EUR: 'de-DE', GBP: 'en-GB' };
const symbolMap = { INR: '₹', USD: '$', EUR: '€', GBP: '£' };
const cur = () => me?.currency || 'INR';
const sym = () => symbolMap[cur()] || '₹';
const money = n => new Intl.NumberFormat(localeMap[cur()] || 'en-IN', { style: 'currency', currency: cur(), maximumFractionDigits: 0 }).format(Number(n || 0));

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

async function api(path, opts = {}) {
  const r = await fetch('/api' + path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
      ...(opts.headers || {})
    }
  });
  if (!r.ok) {
    let m = 'Request failed (' + r.status + ')';
    try {
      const j = await r.json();
      m = j.message || j.error || m;
    } catch {}
    throw Error(m);
  }
  if (r.status === 204) return null;
  if ((r.headers.get('content-type') || '').includes('text/csv')) return r.blob();
  return r.json();
}

function auth(initialMode = 'login', prefillEmail = '', bannerMsg = '') {
  root.innerHTML = `<div class="auth"><form class="authbox" id="authform">
    <div class="brand" style="padding:0 0 8px">Fin<span>Sight</span></div>
    <div class="eyebrow">PERSONAL FINANCE MANAGEMENT SYSTEM</div>
    <div class="authtabs">
      <button type="button" id="tabLogin" class="${initialMode === 'login' ? 'active' : ''}">Sign In</button>
      <button type="button" id="tabRegister" class="${initialMode === 'register' ? 'active' : ''}">New Registration</button>
    </div>
    <h1 id="authtitle">${initialMode === 'register' ? 'New User Registration' : 'Welcome back'}</h1>
    <p class="muted" id="authsub" style="margin:0 0 12px">${initialMode === 'register' ? 'Create a new account — saved permanently in database.' : 'Sign in with your registered email and password.'}</p>
    <div id="authbanner" class="banner ${bannerMsg ? 'ok' : ''}">${esc(bannerMsg)}</div>
    <div id="regfields" style="display:${initialMode === 'register' ? 'block' : 'none'}">
      <label for="regname">Full Name</label>
      <input id="regname" placeholder="Enter your full name">
      <div class="authrow">
        <div><label for="regphone">Phone Number</label><input id="regphone" placeholder="+91 9876543210"></div>
        <div><label for="regcurrency">Currency</label>
          <select id="regcurrency">
            <option value="INR">INR (₹)</option>
            <option value="USD">USD ($)</option>
            <option value="EUR">EUR (€)</option>
            <option value="GBP">GBP (£)</option>
          </select>
        </div>
      </div>
    </div>
    <label for="authemail">Email Address</label>
    <input id="authemail" type="email" required placeholder="you@example.com" value="${esc(prefillEmail)}">
    <label for="authpassword">Password</label>
    <input id="authpassword" type="password" minlength="8" required placeholder="At least 8 characters">
    <div id="confirmwrap" style="display:${initialMode === 'register' ? 'block' : 'none'}">
      <label for="confirmpassword">Confirm Password</label>
      <input id="confirmpassword" type="password" minlength="8" placeholder="Re-enter password">
    </div>
    <div class="error" id="autherror"></div>
    <button type="submit" class="btn" style="width:100%;margin-top:8px" id="authsubmit">${initialMode === 'register' ? 'Register & Save Account' : 'Sign In'}</button>
    <div style="text-align:center"><span class="dbpill" id="usercount">Checking saved accounts...</span></div>
  </form></div>`;

  let signup = initialMode === 'register';
  const setMode = (isReg) => {
    signup = isReg;
    $('tabLogin').classList.toggle('active', !isReg);
    $('tabRegister').classList.toggle('active', isReg);
    $('authtitle').textContent = isReg ? 'New User Registration' : 'Welcome back';
    $('authsub').textContent = isReg
      ? 'Create a new account — saved permanently in database.'
      : 'Sign in with your registered email and password.';
    $('regfields').style.display = isReg ? 'block' : 'none';
    $('confirmwrap').style.display = isReg ? 'block' : 'none';
    $('regname').required = isReg;
    $('confirmpassword').required = isReg;
    $('authsubmit').textContent = isReg ? 'Register & Save Account' : 'Sign In';
    $('autherror').textContent = '';
  };

  setMode(signup);
  $('tabLogin').addEventListener('click', () => setMode(false));
  $('tabRegister').addEventListener('click', () => {
    $('authbanner').className = 'banner';
    setMode(true);
  });

  (async () => {
    try {
      const st = await api('/auth/stats');
      const el = $('usercount');
      if (el) el.textContent = `${st.registeredUsers} registered account(s) saved in H2 database`;
    } catch {
      const el = $('usercount');
      if (el) el.textContent = 'Database ready';
    }
  })();

  $('authform').addEventListener('submit', async e => {
    e.preventDefault();
    const errEl = $('autherror');
    const banEl = $('authbanner');
    const btnEl = $('authsubmit');
    errEl.textContent = '';
    banEl.className = 'banner';

    try {
      const emailVal = $('authemail').value.trim();
      const passVal = $('authpassword').value;

      if (signup) {
        const nameVal = $('regname').value.trim();
        const confirmVal = $('confirmpassword').value;
        const phoneVal = $('regphone').value.trim();
        const currVal = $('regcurrency').value;

        if (!nameVal) {
          errEl.textContent = 'Full name is required.';
          return;
        }
        if (passVal !== confirmVal) {
          errEl.textContent = 'Passwords do not match.';
          return;
        }

        btnEl.disabled = true;
        btnEl.textContent = 'Saving account...';
        const res = await api('/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            name: nameVal,
            email: emailVal,
            password: passVal,
            phone: phoneVal,
            currency: currVal
          })
        });
        const savedEmail = res.user?.email || emailVal;
        const savedName = res.user?.name || nameVal;
        auth('login', savedEmail, `Registration saved! Account for ${savedName} is stored in the database. Sign in below.`);
      } else {
        btnEl.disabled = true;
        btnEl.textContent = 'Signing in...';
        const data = await api('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email: emailVal, password: passVal })
        });
        token = data.token;
        localStorage.setItem('finsight_token', token);
        await start();
      }
    } catch (err) {
      errEl.textContent = err.message || 'Something went wrong. Please try again.';
      btnEl.disabled = false;
      btnEl.textContent = signup ? 'Register & Save Account' : 'Sign In';
    }
  });
}

function shell() {
  root.innerHTML = `<div class="app"><aside class="sidebar"><div class="brand">Fin<span>Sight</span></div><div class="navlabel">WORKSPACE</div><nav class="nav"><button class="active" data-page="dashboard"><b>▦</b>Overview</button><button data-page="transactions"><b>↕</b>Transactions</button><button data-page="budgets"><b>◷</b>Budgets</button><button data-page="goals"><b>◎</b>Saving goals</button><button data-page="analytics"><b>▥</b>Analytics</button><button data-page="reports"><b>⇩</b>Reports</button><button data-page="settings"><b>⚙</b>Settings</button></nav><div class="sidefoot">FinSight · Intelligent Finance<br>Private by design</div></aside><main class="main"><header class="top"><div><div class="eyebrow">YOUR FINANCIAL OVERVIEW</div><h1 id="pagetitle">Dashboard</h1></div><div class="user"><div class="avatar" id="initial">U</div><div class="username"><strong id="username">User</strong><div class="eyebrow" id="useremail"></div></div><button class="btn secondary" id="logout">Log out</button></div></header>
<section id="dashboard" class="section active"><div class="cards" id="metrics"></div><div class="grid2"><div class="panel"><div class="panelhead"><div><h3>Cash flow</h3><div class="sub">Income vs expenses · last 6 months</div></div></div><div class="chartbox"><canvas id="cashchart"></canvas></div></div><div class="panel"><div class="panelhead"><div><h3>Spending by category</h3><div class="sub">All recorded expenses</div></div></div><div class="chartbox"><canvas id="catchart"></canvas></div></div></div><div class="panel" style="margin-bottom:18px"><div class="panelhead"><div><h3>Recent transactions</h3><div class="sub">Your latest financial activity</div></div><button class="btn secondary" data-goto="transactions">View all</button></div><div id="recent"></div></div><div class="panel"><div class="panelhead"><div><h3>Intelligent insights</h3><div class="sub">Signals based on your recorded activity</div></div></div><div class="insights" id="insights"></div></div></section>
<section id="transactions" class="section"><div class="panel"><div class="panelhead"><div><h3>Transaction history</h3><div class="sub">Add, review and manage your income, expenses, and savings contributions</div></div></div><form id="txform" class="formrow"><input id="amount" type="number" min=".01" step=".01" placeholder="Amount (${sym()})" required><select id="type"><option value="EXPENSE">EXPENSE</option><option value="INCOME">INCOME</option></select><select id="category"><option value="Food">Food</option><option value="Transport">Transport</option><option value="Shopping">Shopping</option><option value="Bills">Bills</option><option value="Entertainment">Entertainment</option><option value="Health">Health</option><option value="Education">Education</option><option value="Savings">Savings (Links to Saving Goals)</option><option value="Salary">Salary</option><option value="Freelance">Freelance</option><option value="Other">Other</option></select><input id="description" placeholder="Description (or goal name)"><input id="date" type="date" required><button class="btn">Add transaction</button></form><div class="error" id="txerror"></div><div id="txlist"></div></div></section>
<section id="budgets" class="section"><div class="panel"><div class="panelhead"><div><h3>Monthly budgets</h3><div class="sub">Automatically linked to your recorded EXPENSE transactions in each category</div></div></div><form id="budgetform" class="formrow"><select id="bcat" required><option value="Food">Food</option><option value="Transport">Transport</option><option value="Shopping">Shopping</option><option value="Bills">Bills</option><option value="Entertainment">Entertainment</option><option value="Health">Health</option><option value="Education">Education</option><option value="Other">Other</option></select><input id="blimit" type="number" min="1" placeholder="Monthly limit (${sym()})" required><button class="btn">Set Category Budget</button></form><div id="budgetlist" style="margin-top:22px"></div></div></section>
<section id="goals" class="section"><div class="panel"><div class="panelhead"><div><h3>Your savings goals</h3><div class="sub">Automatically tracks initial saved amount + any "Savings" transactions or matching goal descriptions</div></div></div><form id="goalform" class="formrow"><input id="gname" placeholder="Goal name (e.g. Emergency Reserve Fund)" required><input id="gtarget" type="number" min="1" placeholder="Target amount (${sym()})" required><input id="gcurrent" type="number" min="0" placeholder="Initial saved (${sym()})" value="0"><input id="gdate" type="date"><button class="btn">Add goal</button></form><div id="goallist" style="margin-top:22px"></div></div></section>
<section id="analytics" class="section"><div class="grid2"><div class="panel"><h3>Monthly performance</h3><div class="sub">Six-month comparison</div><div class="chartbox"><canvas id="analyticchart"></canvas></div></div><div class="panel"><h3>Expense forecast</h3><div class="sub">Three-month moving average baseline</div><div class="metric" id="forecast">—</div><p class="muted">A simple historical estimate, not a guarantee.</p><div id="anomalies"></div></div></div><div class="panel"><h3>Category breakdown</h3><div id="categorylist"></div></div></section>
<section id="reports" class="section"><div class="panel"><h3>Monthly report</h3><p class="muted">Export the selected month as a CSV spreadsheet.</p><div class="formrow"><input type="month" id="reportmonth"><button class="btn" id="download">Download CSV</button></div><div class="sub" style="margin-top:15px">Open the downloaded CSV with Excel, Numbers or Google Sheets.</div></div></section>
<section id="settings" class="section">
  <div class="grid2eq">
    <div class="panel">
      <div class="panelhead"><div><h3>Profile & Preferences</h3><div class="sub">Manage your display name, contact phone, currency, and savings target</div></div></div>
      <form id="profileform">
        <div class="field"><label for="setname">Full Name</label><input id="setname" required placeholder="Your full name"></div>
        <div class="field"><label for="setemail">Email Address (Read-only)</label><input id="setemail" disabled style="background:#f5f7fb;color:var(--muted)"></div>
        <div class="field"><label for="setphone">Phone Number</label><input id="setphone" placeholder="+91 9876543210"></div>
        <div class="field"><label for="setcurrency">Preferred Currency</label>
          <select id="setcurrency">
            <option value="INR">INR (₹) — Indian Rupee</option>
            <option value="USD">USD ($) — US Dollar</option>
            <option value="EUR">EUR (€) — Euro</option>
            <option value="GBP">GBP (£) — British Pound</option>
          </select>
        </div>
        <div class="field"><label for="setsavings">Monthly Savings Target (% of Income)</label><input id="setsavings" type="number" min="0" max="90" required></div>
        <button class="btn" type="submit">Save Preferences</button>
        <div id="profilestatus" class="success"></div>
      </form>
    </div>
    <div class="panel">
      <div class="panelhead"><div><h3>Security & Password</h3><div class="sub">Update your account password (BCrypt encrypted)</div></div></div>
      <form id="passwordform">
        <div class="field"><label for="curpass">Current Password</label><input id="curpass" type="password" required placeholder="Enter current password"></div>
        <div class="field"><label for="newpass">New Password</label><input id="newpass" type="password" minlength="8" required placeholder="At least 8 characters"></div>
        <div class="field"><label for="confirmpass">Confirm New Password</label><input id="confirmpass" type="password" minlength="8" required placeholder="Re-enter new password"></div>
        <button class="btn" type="submit">Update Password</button>
        <div id="passstatus" class="success"></div>
      </form>
    </div>
  </div>
  <div class="panel">
    <div class="panelhead"><div><h3>Account & Data Management</h3><div class="sub" id="accountmeta">Account details and workspace reset controls</div></div></div>
    <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
      <button class="btn secondary" id="cleardata">Clear All Financial Data</button>
      <button class="btn danger" id="deleteaccount">Delete Account Permanently</button>
    </div>
    <div class="sub" style="margin-top:10px">Clearing data deletes all transactions, budgets, and goals while keeping your login active.</div>
  </div>
</section>
</main></div>`;

  document.querySelectorAll('[data-page]').forEach(b => b.onclick = () => navigate(b.dataset.page));
  document.querySelectorAll('[data-goto]').forEach(b => b.onclick = () => navigate(b.dataset.goto));
  $('logout').onclick = () => {
    token = null;
    localStorage.removeItem('finsight_token');
    Object.values(charts).forEach(c => c.destroy());
    auth();
  };

  $('date').value = new Date().toISOString().slice(0, 10);
  $('reportmonth').value = new Date().toISOString().slice(0, 7);

  $('txform').onsubmit = async e => {
    e.preventDefault();
    $('txerror').textContent = '';
    try {
      await api('/transactions', {
        method: 'POST',
        body: JSON.stringify({
          amount: Number($('amount').value),
          type: $('type').value,
          category: $('category').value,
          description: $('description').value,
          transactionDate: $('date').value
        })
      });
      e.target.reset();
      $('date').value = new Date().toISOString().slice(0, 10);
      await loadAll();
    } catch (err) {
      $('txerror').textContent = err.message;
    }
  };

  $('budgetform').onsubmit = async e => {
    e.preventDefault();
    try {
      const d = new Date();
      await api('/budgets', {
        method: 'POST',
        body: JSON.stringify({
          category: $('bcat').value,
          limitAmount: Number($('blimit').value),
          month: d.getMonth() + 1,
          year: d.getFullYear()
        })
      });
      $('blimit').value = '';
      await loadBudgets();
    } catch (err) {
      alert(err.message);
    }
  };

  $('goalform').onsubmit = async e => {
    e.preventDefault();
    try {
      await api('/goals', {
        method: 'POST',
        body: JSON.stringify({
          name: $('gname').value,
          targetAmount: Number($('gtarget').value),
          currentAmount: Number($('gcurrent').value),
          targetDate: $('gdate').value || null
        })
      });
      e.target.reset();
      $('gcurrent').value = '0';
      await loadGoals();
    } catch (err) {
      alert(err.message);
    }
  };

  $('download').onclick = async () => {
    try {
      const blob = await api('/reports/monthly?month=' + $('reportmonth').value);
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'finsight-report.csv';
      a.click();
      URL.revokeObjectURL(a.href);
    } catch (e) {
      alert(e.message);
    }
  };

  $('profileform').onsubmit = async e => {
    e.preventDefault();
    const st = $('profilestatus');
    st.className = 'success';
    st.textContent = '';
    try {
      me = await api('/settings/profile', {
        method: 'PUT',
        body: JSON.stringify({
          name: $('setname').value.trim(),
          phone: $('setphone').value.trim(),
          currency: $('setcurrency').value,
          savingsTargetPercent: Number($('setsavings').value)
        })
      });
      syncHeader();
      await loadAll();
      st.textContent = 'Preferences saved successfully.';
    } catch (err) {
      st.className = 'error';
      st.textContent = err.message;
    }
  };

  $('passwordform').onsubmit = async e => {
    e.preventDefault();
    const st = $('passstatus');
    st.className = 'success';
    st.textContent = '';
    if ($('newpass').value !== $('confirmpass').value) {
      st.className = 'error';
      st.textContent = 'New passwords do not match.';
      return;
    }
    try {
      const res = await api('/settings/password', {
        method: 'PUT',
        body: JSON.stringify({
          currentPassword: $('curpass').value,
          newPassword: $('newpass').value
        })
      });
      e.target.reset();
      st.textContent = res.message || 'Password updated.';
    } catch (err) {
      st.className = 'error';
      st.textContent = err.message;
    }
  };

  $('cleardata').onclick = async () => {
    if (!confirm('Delete all your transactions, budgets, and savings goals? This cannot be undone.')) return;
    try {
      await api('/settings/data', { method: 'DELETE' });
      await loadAll();
      alert('All financial data has been cleared.');
    } catch (err) {
      alert(err.message);
    }
  };

  $('deleteaccount').onclick = async () => {
    if (!confirm('Permanently delete your account and all data?')) return;
    try {
      await api('/settings/account', { method: 'DELETE' });
      token = null;
      localStorage.removeItem('finsight_token');
      Object.values(charts).forEach(c => c.destroy());
      auth();
    } catch (err) {
      alert(err.message);
    }
  };
}

function syncHeader() {
  $('username').textContent = me.name;
  $('useremail').textContent = me.email;
  $('initial').textContent = me.name.slice(0, 1).toUpperCase();
  $('setname').value = me.name;
  $('setemail').value = me.email;
  $('setphone').value = me.phone || '';
  $('setcurrency').value = me.currency || 'INR';
  $('setsavings').value = me.savingsTargetPercent ?? 20;
  $('accountmeta').textContent = `User ID: #${me.id} · Role: ${me.role} · Registered: ${me.createdAt || 'Today'} · Active Currency: ${cur()}`;
  $('amount').placeholder = `Amount (${sym()})`;
  $('blimit').placeholder = `Monthly limit (${sym()})`;
  $('gtarget').placeholder = `Target amount (${sym()})`;
  $('gcurrent').placeholder = `Initial saved (${sym()})`;
}

function navigate(page) {
  document.querySelectorAll('.section').forEach(s => s.classList.toggle('active', s.id === page));
  document.querySelectorAll('[data-page]').forEach(b => b.classList.toggle('active', b.dataset.page === page));
  $('pagetitle').textContent = ({ dashboard: 'Dashboard', transactions: 'Transactions', budgets: 'Budgets', goals: 'Saving goals', analytics: 'Analytics', reports: 'Reports', settings: 'Settings' })[page];
}

function table(rows) {
  if (!rows.length) return '<div class="empty">No transactions yet. Add one above to get started.</div>';
  return `<table class="table"><thead><tr><th>Transaction</th><th>Category</th><th>Date</th><th>Amount</th><th></th></tr></thead><tbody>${rows.map(t => `<tr><td><strong>${esc(t.description || t.category)}</strong><div class="sub">${t.type.toLowerCase()}</div></td><td>${esc(t.category)}</td><td>${t.transactionDate}</td><td style="font-weight:700;color:${t.type === 'INCOME' ? 'var(--green)' : 'var(--ink)'}">${t.type === 'INCOME' ? '+' : '−'}${money(t.amount)}</td><td><button onclick="removeTx(${t.id})" style="background:none;color:#a4adbd">Delete</button></td></tr>`).join('')}</tbody></table>`;
}

async function removeTx(id) {
  if (!confirm('Delete this transaction?')) return;
  try {
    await api('/transactions/' + id, { method: 'DELETE' });
    await loadAll();
  } catch (e) {
    alert(e.message);
  }
}

async function removeBudget(id) {
  if (!confirm('Delete this budget?')) return;
  try {
    await api('/budgets/' + id, { method: 'DELETE' });
    await loadBudgets();
  } catch (e) {
    alert(e.message);
  }
}

async function removeGoal(id) {
  if (!confirm('Delete this savings goal?')) return;
  try {
    await api('/goals/' + id, { method: 'DELETE' });
    await loadGoals();
  } catch (e) {
    alert(e.message);
  }
}

async function loadAll() {
  [txs] = await Promise.all([api('/transactions')]);
  $('txlist').innerHTML = table(txs);
  $('recent').innerHTML = table(txs.slice(0, 6));
  const d = await api('/dashboard');
  $('metrics').innerHTML = [
    ['Total balance', money(d.balance), 'Across all recorded activity'],
    ['Income this month', money(d.income), 'Money received'],
    ['Expenses this month', money(d.expenses), 'Money spent'],
    ['Net savings this month', money(d.savings), 'Income minus expenses']
  ].map(x => `<div class="card"><div class="metric-label">${x[0]}</div><div class="metric">${x[1]}</div><div class="delta">${x[2]}</div></div>`).join('');
  await loadCharts();
  await loadBudgets();
  await loadGoals();
  await loadInsights(d);
}

function chart(id, config) {
  if (charts[id]) charts[id].destroy();
  const el = $(id);
  if (el) charts[id] = new Chart(el, config);
}

async function loadCharts() {
  const m = await api('/analytics/monthly');
  const c = await api('/analytics/categories');
  chart('cashchart', {
    type: 'bar',
    data: {
      labels: m.months.map(x => x.month),
      datasets: [
        { label: 'Income', data: m.months.map(x => x.income), backgroundColor: '#4263eb', borderRadius: 5 },
        { label: 'Expenses', data: m.months.map(x => x.expenses), backgroundColor: '#a9b8f8', borderRadius: 5 }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { grid: { color: '#f0f2f6' }, ticks: { callback: v => sym() + v } }, x: { grid: { display: false } } } }
  });
  chart('catchart', {
    type: 'doughnut',
    data: { labels: c.map(x => x.category), datasets: [{ data: c.map(x => x.amount), backgroundColor: ['#4263eb', '#7c91f4', '#16a879', '#f3b65b', '#e47b89', '#8b79c9', '#55b6c4'], borderWidth: 0 }] },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
  });
  chart('analyticchart', {
    type: 'line',
    data: {
      labels: m.months.map(x => x.month),
      datasets: [
        { label: 'Income', data: m.months.map(x => x.income), borderColor: '#4263eb', tension: .35 },
        { label: 'Expenses', data: m.months.map(x => x.expenses), borderColor: '#16a879', tension: .35 }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
  });
  $('categorylist').innerHTML = c.length
    ? c.map(x => `<div style="display:flex;justify-content:space-between;padding:12px;border-bottom:1px solid var(--line)"><span>${esc(x.category)}</span><strong>${money(x.amount)}</strong></div>`).join('')
    : '<div class="empty">Add expense transactions to see category insights.</div>';
}

async function loadBudgets() {
  budgets = await api('/budgets');
  $('budgetlist').innerHTML = budgets.length
    ? budgets.map(b => {
        const limit = Number(b.limitAmount || 0);
        const spent = Number(b.spent || 0);
        const rem = Number(b.remaining || 0);
        const pct = limit > 0 ? Math.round((spent / limit) * 100) : 0;
        const barW = Math.min(100, pct);
        const isOver = spent > limit;
        const linked = Array.isArray(b.transactions) ? b.transactions : [];
        const linkedHtml = linked.length
          ? `<div class="linkedbox"><div class="sub" style="margin:0 0 6px;font-weight:700">Linked ${esc(b.category)} Expenses (${linked.length}):</div>${linked.map(t => `<div class="linkeditem"><span>${esc(t.description || t.category)} <span class="muted">(${t.transactionDate})</span></span><strong>−${money(t.amount)}</strong></div>`).join('')}</div>`
          : `<div class="sub" style="margin-top:8px">No ${esc(b.category)} expense transactions recorded yet.</div>`;

        return `<div style="margin:20px 0;padding-bottom:18px;border-bottom:1px solid var(--line)">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:9px">
            <div><strong>${esc(b.category)}</strong> <span class="pill ${isOver ? 'out' : ''}" style="margin-left:8px">${pct}% used</span></div>
            <div style="display:flex;align-items:center;gap:12px">
              <span><strong>${money(spent)}</strong> / ${money(limit)}</span>
              <button onclick="removeBudget(${b.id})" style="background:none;color:#a4adbd;font-size:12px">Delete</button>
            </div>
          </div>
          <div class="progress ${isOver ? 'over' : ''}"><span style="width:${barW}%"></span></div>
          <div class="sub" style="margin-top:6px;color:${isOver ? 'var(--red)' : 'var(--muted)'}">${isOver ? 'Over budget by ' + money(Math.abs(rem)) : money(rem) + ' remaining'}</div>
          ${linkedHtml}
        </div>`;
      }).join('')
    : '<div class="empty">No budgets yet. Select a category (e.g., Food) and set a monthly limit above.</div>';
}

async function loadGoals() {
  goals = await api('/goals');
  $('goallist').innerHTML = goals.length
    ? goals.map(g => {
        const target = Number(g.targetAmount || 0);
        const current = Number(g.currentAmount || 0);
        const baseAmt = Number(g.baseAmount || 0);
        const contribAmt = Number(g.contributedAmount || 0);
        const pct = target > 0 ? Math.round((current / target) * 100) : 0;
        const barW = Math.min(100, pct);
        const isDone = current >= target && target > 0;
        const linked = Array.isArray(g.transactions) ? g.transactions : [];
        const linkedHtml = linked.length
          ? `<div class="linkedbox"><div class="sub" style="margin:0 0 6px;font-weight:700">Linked Savings Transactions (${linked.length} · +${money(contribAmt)}):</div>${linked.map(t => `<div class="linkeditem"><span>${esc(t.description || t.category)} <span class="muted">(${t.transactionDate})</span></span><strong style="color:var(--green)">+${money(t.amount)}</strong></div>`).join('')}</div>`
          : `<div class="sub" style="margin-top:8px">Tip: Add a transaction with category <strong>Savings</strong> or mention <strong>${esc(g.name)}</strong> in description to auto-link it here.</div>`;

        return `<div style="margin:20px 0;padding-bottom:18px;border-bottom:1px solid var(--line)">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div><strong>${esc(g.name)}</strong> <span class="pill" style="margin-left:8px">${pct}%</span></div>
            <div style="display:flex;align-items:center;gap:12px">
              <strong>${money(current)} / ${money(target)}</strong>
              <button onclick="removeGoal(${g.id})" style="background:none;color:#a4adbd;font-size:12px">Delete</button>
            </div>
          </div>
          <p class="sub">Initial: ${money(baseAmt)} + Linked Transactions: ${money(contribAmt)} · Deadline: ${g.targetDate || 'No deadline'}</p>
          <div class="progress ${isDone ? 'done' : ''}"><span style="width:${barW}%"></span></div>
          ${linkedHtml}
        </div>`;
      }).join('')
    : '<div class="empty">Create a savings goal above to track progress automatically.</div>';
}

async function loadInsights(d) {
  const [f, a] = await Promise.all([api('/insights/forecast'), api('/insights/anomalies')]);
  $('forecast').textContent = money(f.forecast);
  $('anomalies').innerHTML = a.length
    ? '<strong>Unusual transactions to review</strong>' + a.slice(0, 4).map(x => `<p class="sub">${esc(x.category)} · ${money(x.amount)} · ${x.date} (z=${x.zScore})</p>`).join('')
    : '<p class="sub">No unusual transactions detected from available history.</p>';
  const target = me?.savingsTargetPercent ?? 20;
  const inc = Number(d?.income || 0), sav = Number(d?.savings || 0);
  const rate = inc > 0 ? Math.round((sav / inc) * 100) : 0;
  const goalMsg = inc > 0
    ? `Current monthly savings rate is ${rate}% against your ${target}% target.`
    : `Your savings target is set to ${target}% of monthly income (configurable in Settings).`;
  $('insights').innerHTML = `<div class="insight">↗<strong>Monthly forecast</strong><span class="muted">Estimated next-month expenses: ${money(f.forecast)} using a 3-month average.</span></div><div class="insight">◉<strong>Spending review</strong><span class="muted">${a.length ? a.length + ' unusual transaction(s) flagged for your review.' : 'No unusually high category transactions flagged.'}</span></div><div class="insight">✓<strong>Savings target (${target}%)</strong><span class="muted">${goalMsg}</span></div>`;
}

async function start() {
  try {
    me = await api('/me');
    shell();
    syncHeader();
    await loadAll();
  } catch (e) {
    token = null;
    localStorage.removeItem('finsight_token');
    auth();
  }
}

if (token) start(); else auth();
