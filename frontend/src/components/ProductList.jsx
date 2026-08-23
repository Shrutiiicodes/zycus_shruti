import { usePolling } from '../hooks/usePolling.js';
import { api } from '../api/client.js';
import ProductRow from './ProductRow.jsx';

export default function ProductList({ registerRefresh }) {
  const { data: products, loading, error, refresh } = usePolling(api.listProducts, 5000);
  registerRefresh(refresh);

  return (
    <section>
      <h2 style={{ fontSize: '16px', marginBottom: '10px' }}>Catalog</h2>
      {loading && !products && <p style={{ color: '#6b7280' }}>Loading…</p>}
      {error && <p style={{ color: '#9b1c1c' }}>Error: {error}</p>}
      {products && (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid #dcdfe4', color: '#6b7280' }}>
              <th style={{ padding: '6px 4px' }}>ID</th>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th>Stock / threshold</th>
              <th>Velocity</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {products.map(p => <ProductRow key={p.id} product={p} onChanged={refresh} />)}
          </tbody>
        </table>
      )}
    </section>
  );
}