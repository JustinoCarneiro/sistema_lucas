
const axios = require('axios');

async function debug() {
  try {
    console.log('Logging in as lucas@email.com...');
    const loginRes = await axios.post('http://localhost:8081/auth/login', {
      email: 'lucas@email.com',
      password: '123456'
    });
    const token = loginRes.data.token;
    console.log('Token received.');

    console.log('Fetching /consultas/minhas...');
    const res = await axios.get('http://localhost:8081/consultas/minhas', {
      headers: { Authorization: `Bearer ${token}` }
    });

    console.log('Appointments Count:', res.data.length);
    res.data.forEach(a => {
      console.log(`ID: ${a.id} | Professional: ${a.professionalName} | Status: ${a.status} | podeCancelar: ${a.podeCancelar} | isLate: ${a.isLate} | Date: ${a.startTime}`);
    });
  } catch (err) {
    console.error('Error:', err.response?.data || err.message);
  }
}

debug();
