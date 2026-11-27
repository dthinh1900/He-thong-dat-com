/** Filter and group the report contract shared by both data adapters. */
const toUtc = key => new Date(`${key}T12:00:00Z`);
export const addDays = (key, offset) => {
  const d = toUtc(key);
  d.setUTCDate(d.getUTCDate() + offset);
  return d.toISOString().slice(0, 10);
};
export const dayCount = (from, to) => Math.round((toUtc(to) - toUtc(from)) / 86400000) + 1;
export function periodBounds({ mode, month, day, from, to }) {
  if (mode === 'day') return { from: day, to: day };
  if (mode === 'range') return { from, to };
  const last = new Date(Date.UTC(Number(month.slice(0, 4)), Number(month.slice(5, 7)), 0)).getUTCDate();
  return { from: `${month}-01`, to: `${month}-${String(last).padStart(2, '0')}` };
}
export function previousBounds(from, to) {
  const count = dayCount(from, to);
  return { from: addDays(from, -count), to: addDays(from, -1) };
}
const addMoney = (first, second) => Math.round((first + second) * 100) / 100;
export const ticketSource = t => t.transferId ? 'TRANSFER' : t.purchaseId ? 'PURCHASE' : 'REGISTRATION';
export const matchesPerson = (person, query) => !query || `${person?.name || ''} ${person?.code || ''}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
export function selectTickets(state, { from, to, department = 'all', type = 'all', source = 'all', query = '' }) {
  const people = new Map((state.employees || []).map(p => [String(p.id), p]));
  return (state.tickets || [])
    .filter(t => t.date >= from && t.date <= to && (type === 'all' || t.type === type)
      && (source === 'all' || ticketSource(t) === source))
    .flatMap(t => {
      const person = people.get(String(t.userId));
      if (!person || (department !== 'all' && String(person.departmentId) !== String(department)) || !matchesPerson(person, query)) return [];
      return [{ ...t, owner: person, amount: Number(t.price) }];
    });
}
export function summarizeTickets(tickets) {
  return tickets.reduce((sum, t) => {
    sum.total += 1;
    sum.amount = addMoney(sum.amount, t.amount);
    if (t.type === 'SPECIAL') sum.special += 1;
    else sum.normal += 1;
    if (t.purchaseId) sum.purchased += 1;
    if (t.transferId) sum.received += 1;
    return sum;
  }, { total: 0, amount: 0, normal: 0, special: 0, purchased: 0, received: 0 });
}
export function buildDaily(tickets, from, to) {
  const byDay = new Map();
  for (const t of tickets) {
    const v = byDay.get(t.date) || { date: t.date, tickets: 0, totalAmount: 0 };
    v.tickets += 1; v.totalAmount = addMoney(v.totalAmount, t.amount); byDay.set(t.date, v);
  }
  const days = [];
  for (let d = from; d <= to; d = addDays(d, 1)) {
    days.push(byDay.get(d) || { date: d, tickets: 0, totalAmount: 0 });
  }
  return days;
}
export function buildDepartments(state, tickets) {
  const map = new Map((state.departments || []).map(d => [String(d.id), { ...d, tickets: 0, amount: 0, normal: 0, special: 0, people: new Set() }]));
  for (const t of tickets) {
    const d = map.get(String(t.owner.departmentId));
    if (!d) continue;
    d.tickets += 1; d.amount = addMoney(d.amount, t.amount); d.people.add(String(t.owner.id));
    if (t.type === 'SPECIAL') d.special += 1; else d.normal += 1;
  }
  return [...map.values()].map(d => ({ ...d, people: d.people.size }))
    .filter(d => d.tickets > 0).sort((a, b) => b.tickets - a.tickets || a.name.localeCompare(b.name, 'vi'));
}
export function buildEmployees(tickets, state, { from, to, department, type, source, query }) {
  const map = new Map();
  const rowFor = person => {
    const key = String(person.id);
    if (!map.has(key)) map.set(key, { person, tickets: 0, amount: 0, normal: 0, special: 0, purchased: 0, received: 0 });
    return map.get(key);
  };
  for (const t of tickets) {
    const row = rowFor(t.owner);
    row.tickets += 1; row.amount = addMoney(row.amount, t.amount);
    if (t.type === 'SPECIAL') row.special += 1; else row.normal += 1;
    if (t.purchaseId) row.purchased += 1;
    if (t.transferId) row.received += 1;
  }
  if (source === 'all' || source === 'TRANSFER') {
    const people = new Map(state.employees.map(person => [String(person.id), person]));
    for (const transaction of state.transactions) {
      if (transaction.date < from || transaction.date > to || (type !== 'all' && transaction.type !== type)) continue;
      for (const id of [transaction.fromId, transaction.toId]) {
        const person = people.get(String(id));
        if (person && (department === 'all' || String(person.departmentId) === department) && matchesPerson(person, query)) {
          rowFor(person);
        }
      }
    }
  }
  return [...map.values()].sort((a, b) => b.tickets - a.tickets || a.person.name.localeCompare(b.person.name, 'vi'));
}
export function transferActivity(state, { from, to, department = 'all', type = 'all', query = '' }) {
  const people = new Map((state.employees || []).map(p => [String(p.id), p]));
  return (state.transactions || []).filter(x => {
    const buyer = people.get(String(x.toId));
    return x.date >= from && x.date <= to &&
      (department === 'all' || String(buyer?.departmentId) === String(department)) &&
      (type === 'all' || x.type === type) && matchesPerson(buyer, query);
  }).reduce((sum, x) => sum + Number(x.quantity || 1), 0);
}
export function employeeTransfers(state, id, { from, to, type }) {
  return (state.transactions || []).filter(x => x.date >= from && x.date <= to &&
    (type === 'all' || x.type === type) &&
    (String(x.fromId) === String(id) || String(x.toId) === String(id)))
    .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
}
export const pctChange = (now, previous) => previous ? ((now - previous) / previous) * 100 : (now ? null : 0);
