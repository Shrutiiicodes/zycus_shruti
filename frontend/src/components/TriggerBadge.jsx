const STYLES = {
  INVENTORY_LOW: { background: '#fde8e8', color: '#9b1c1c', label: 'Inventory low' },
  DEMAND_SPIKE: { background: '#e0f2e9', color: '#0f6b3a', label: 'Demand spike' },
  MANUAL: { background: '#eef0f2', color: '#3d4451', label: 'Manual' },
  INITIAL: { background: '#eaf1fc', color: '#1c4b9b', label: 'Initial' }
};

export default function TriggerBadge({ reason }) {
  const style = STYLES[reason] || STYLES.MANUAL;
  return (
    <span style={{
      background: style.background,
      color: style.color,
      fontSize: '11px',
      fontWeight: 600,
      padding: '2px 8px',
      borderRadius: '4px',
      letterSpacing: '0.02em'
    }}>
      {style.label}
    </span>
  );
}