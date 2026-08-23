import TriggerBadge from './TriggerBadge.jsx';

export default function SuggestionCard({ type, suggestion, onDecide, deciding }) {
  const isPricing = type === 'pricing';

  return (
    <div style={{
      border: '1px solid #dcdfe4', borderRadius: '6px', padding: '12px 14px', marginBottom: '10px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
        <div>
          <strong>{isPricing ? 'Pricing suggestion' : 'Reorder suggestion'}</strong>{' '}
          <span style={{ color: '#6b7280', fontSize: '13px' }}>· product {suggestion.productId}</span>
        </div>
        <TriggerBadge reason={suggestion.triggerReason} />
      </div>

      {isPricing ? (
        <div style={{ fontSize: '14px', marginBottom: '6px' }}>
          {suggestion.currentPrice} → <strong>{suggestion.recommendedPrice}</strong>{' '}
          <span style={{ color: '#6b7280' }}>({suggestion.direction}, confidence {suggestion.confidence.toFixed(2)})</span>
        </div>
      ) : (
        <div style={{ fontSize: '14px', marginBottom: '6px' }}>
          Reorder <strong>{suggestion.recommendedQuantity} units</strong>{' '}
          <span style={{ color: '#6b7280' }}>(lead time {suggestion.suggestedLeadTimeDays}d, confidence {suggestion.confidence.toFixed(2)})</span>
        </div>
      )}

      <p style={{ fontSize: '13px', color: '#4b5563', margin: '0 0 10px' }}>{suggestion.reasoning}</p>

      <div style={{ display: 'flex', gap: '8px' }}>
        <button disabled={deciding} onClick={() => onDecide(type, suggestion.id, 'ACCEPT')}
                style={{ padding: '5px 12px', background: '#0f6b3a', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          Accept
        </button>
        <button disabled={deciding} onClick={() => onDecide(type, suggestion.id, 'REJECT')}
                style={{ padding: '5px 12px', background: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer' }}>
          Reject
        </button>
      </div>
    </div>
  );
}