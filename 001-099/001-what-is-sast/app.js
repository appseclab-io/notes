const express = require('express');
const { exec } = require('child_process');
const db = require('./db');

const app = express();

// 1. SQL built by joining strings
app.get('/users', async (req, res) => {
  const rows = await db.query("SELECT * FROM users WHERE name = '" + req.query.name + "'");
  res.json(rows);
});

// 2. User input in a shell command
app.get('/ping', (req, res) => {
  exec('ping -c 1 ' + req.query.host, (err, out) => res.send(out));
});

// 3. The same SQL injection as 1, one function call away
function byName(name) {
  return "SELECT * FROM users WHERE name = '" + name + "'";
}
app.get('/people', async (req, res) => {
  res.json(await db.query(byName(req.query.name)));
});

// 4. Any signed-in user reads any invoice: no ownership check
app.get('/invoices/:id', async (req, res) => {
  res.json(await db.query('SELECT * FROM invoices WHERE id = $1', [req.params.id]));
});

app.listen(3000);
