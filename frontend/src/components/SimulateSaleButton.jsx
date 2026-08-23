import { useState } from 'react';
import { api } from '../api/client.js';

export default function SimulateSaleButton({ productId, onDone }) {
  const [busy, setBusy] = useState(false);

  async function handleClick() {
    setBusy(true);
    try {
      await api.placeOrder(productId, 1);
      onDone();
    } catch (err) {
      alert(`Could not simulate sale: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  return (
    <button disabled={busy} onClick={handleClick}
            style={{ padding: '3px 10px', fontSize: '12px', background: '#eaf1fc', color: '#1c4b9b', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
      {busy ? '…' : 'Simulate sale'}
    </button>
  );
}