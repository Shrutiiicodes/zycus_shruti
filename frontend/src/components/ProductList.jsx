import { useState } from 'react';
import { usePolling } from '../hooks/usePolling.js';
import { api } from '../api/client.js';
import ProductRow from './ProductRow.jsx';

export default function ProductList({ registerRefresh }) {
  const { data: products, loading, error, refresh } = usePolling(api.listProducts, 5000);
  registerRefresh(refresh);

  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    sku: '',
    name: '',
    category: 'ELECTRONICS',
    currentPrice: '',
    stockLevel: '',
    reorderThreshold: '',
    demandVelocity: '0'
  });
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState(null);

  const handleCreate = async (e) => {
    e.preventDefault();
    setCreating(true);
    setCreateError(null);
    try {
      await api.createProduct({
        sku: formData.sku,
        name: formData.name,
        category: formData.category,
        currentPrice: parseFloat(formData.currentPrice),
        stockLevel: parseInt(formData.stockLevel, 10),
        reorderThreshold: parseInt(formData.reorderThreshold, 10),
        demandVelocity: parseInt(formData.demandVelocity || '0', 10)
      });
      setFormData({
        sku: '',
        name: '',
        category: 'ELECTRONICS',
        currentPrice: '',
        stockLevel: '',
        reorderThreshold: '',
        demandVelocity: '0'
      });
      setShowAddForm(false);
      refresh();
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreating(false);
    }
  };

  return (
    <section>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
        <h2 style={{ fontSize: '16px', margin: 0 }}>Catalog</h2>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          style={{
            background: showAddForm ? '#6b7280' : '#1a6b5a',
            color: '#ffffff',
            border: 'none',
            borderRadius: '4px',
            padding: '6px 12px',
            fontSize: '12px',
            fontWeight: '500',
            cursor: 'pointer'
          }}
        >
          {showAddForm ? 'Cancel' : '+ Add Product'}
        </button>
      </div>

      {showAddForm && (
        <form onSubmit={handleCreate} style={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '16px', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '13px', fontWeight: '600', marginBottom: '12px', color: '#111827' }}>Add New Product</h3>
          {createError && <p style={{ color: '#dc2626', fontSize: '12px', marginBottom: '8px' }}>{createError}</p>}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '10px', marginBottom: '12px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>SKU</label>
              <input
                required
                placeholder="SKU-100"
                value={formData.sku}
                onChange={e => setFormData({ ...formData, sku: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Name</label>
              <input
                required
                placeholder="Product Name"
                value={formData.name}
                onChange={e => setFormData({ ...formData, name: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Category</label>
              <select
                value={formData.category}
                onChange={e => setFormData({ ...formData, category: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              >
                <option value="ELECTRONICS">ELECTRONICS</option>
                <option value="APPAREL">APPAREL</option>
                <option value="HOME">HOME</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Price ($)</label>
              <input
                required
                type="number"
                step="0.01"
                placeholder="29.99"
                value={formData.currentPrice}
                onChange={e => setFormData({ ...formData, currentPrice: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Stock</label>
              <input
                required
                type="number"
                placeholder="10"
                value={formData.stockLevel}
                onChange={e => setFormData({ ...formData, stockLevel: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Threshold</label>
              <input
                required
                type="number"
                placeholder="15"
                value={formData.reorderThreshold}
                onChange={e => setFormData({ ...formData, reorderThreshold: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '11px', color: '#4b5563', marginBottom: '4px' }}>Velocity (orders/24h)</label>
              <input
                type="number"
                placeholder="0"
                value={formData.demandVelocity}
                onChange={e => setFormData({ ...formData, demandVelocity: e.target.value })}
                style={{ width: '100%', padding: '6px', fontSize: '12px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={creating}
            style={{
              background: '#c45a2a',
              color: '#ffffff',
              border: 'none',
              borderRadius: '4px',
              padding: '6px 16px',
              fontSize: '12px',
              fontWeight: '500',
              cursor: creating ? 'not-allowed' : 'pointer'
            }}
          >
            {creating ? 'Saving…' : 'Save Product'}
          </button>
        </form>
      )}

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