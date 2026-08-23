import SimulateSaleButton from './SimulateSaleButton.jsx';

export default function ProductRow({ product, onChanged }) {
  return (
    <tr>
      <td>{product.id}</td>
      <td>{product.name}</td>
      <td>{product.category}</td>
      <td>{product.currentPrice}</td>
      <td>{product.stockLevel} <span style={{ color: '#9ca3af' }}>/ {product.reorderThreshold}</span></td>
      <td>{product.demandVelocity}</td>
      <td>
        <span style={{
          fontSize: '11px', fontWeight: 600, padding: '2px 6px', borderRadius: '4px',
          background: product.status === 'OUT_OF_STOCK' ? '#fde8e8'
            : product.status === 'PRICE_REVIEW_PENDING' ? '#fff4e0' : '#eef4ec',
          color: product.status === 'OUT_OF_STOCK' ? '#9b1c1c'
            : product.status === 'PRICE_REVIEW_PENDING' ? '#92600a' : '#276b3d'
        }}>
          {product.status}
        </span>
      </td>
      <td><SimulateSaleButton productId={product.id} onDone={onChanged} /></td>
    </tr>
  );
}