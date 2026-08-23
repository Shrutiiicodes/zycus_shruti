import { useState } from 'react';
import { usePolling } from '../hooks/usePolling.js';
import { api } from '../api/client.js';
import SuggestionCard from './SuggestionCard.jsx';

export default function SuggestionBoard({ onProductsChanged }) {
  const { data: pricing, loading: loadingPricing, error: errorPricing, refresh: refreshPricing } =
    usePolling(api.listPendingPricing, 4000);
  const { data: reorder, loading: loadingReorder, error: errorReorder, refresh: refreshReorder } =
    usePolling(api.listPendingReorder, 4000);

  const [decidingId, setDecidingId] = useState(null);

  async function handleDecide(type, id, decision) {
    setDecidingId(id);
    try {
      if (type === 'pricing') {
        await api.decidePricing(id, decision);
        await refreshPricing();
      } else {
        await api.decideReorder(id, decision);
        await refreshReorder();
      }
      onProductsChanged();
    } catch (err) {
      alert(`Could not ${decision.toLowerCase()}: ${err.message}`);
    } finally {
      setDecidingId(null);
    }
  }

  const loading = loadingPricing || loadingReorder;
  const error = errorPricing || errorReorder;
  const suggestions = [
    ...(pricing || []).map(s => ({ type: 'pricing', suggestion: s })),
    ...(reorder || []).map(s => ({ type: 'reorder', suggestion: s }))
  ];

  return (
    <section>
      <h2 style={{ fontSize: '16px', marginBottom: '10px' }}>Pending suggestions</h2>
      {loading && suggestions.length === 0 && <p style={{ color: '#6b7280' }}>Loading…</p>}
      {error && <p style={{ color: '#9b1c1c' }}>Error: {error}</p>}
      {!loading && suggestions.length === 0 && !error && (
        <p style={{ color: '#6b7280', fontSize: '13px' }}>No pending suggestions right now.</p>
      )}
      {suggestions.map(({ type, suggestion }) => (
        <SuggestionCard
          key={`${type}-${suggestion.id}`}
          type={type}
          suggestion={suggestion}
          onDecide={handleDecide}
          deciding={decidingId === suggestion.id}
        />
      ))}
    </section>
  );
}