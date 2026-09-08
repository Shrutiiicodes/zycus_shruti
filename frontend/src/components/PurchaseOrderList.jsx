import { useState } from 'react';
import { usePolling } from '../hooks/usePolling.js';
import { api } from '../api/client.js';

export default function PurchaseOrderList() {
  const { data: pos, loading, error, refresh } = usePolling(api.listPurchaseOrders, 4000);
  const [receivingId, setReceivingId] = useState(null);

  const handleReceive = async (id) => {
    setReceivingId(id);
    try {
      await api.receiveShipment(id);
      refresh();
    } catch (err) {
      alert('Error receiving shipment: ' + err.message);
    } finally {
      setReceivingId(null);
    }
  };

  return (
    <div style={{ background: '#ffffff', border: '1px solid #dcdfe4', borderRadius: '6px', padding: '20px' }}>
      <h2 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '14px', color: '#1a1814' }}>
        Purchase Orders & Supplier Replenishment
      </h2>

      {loading && !pos && <p style={{ color: '#6b7280', fontSize: '13px' }}>Loading purchase orders…</p>}
      {error && <p style={{ color: '#9b1c1c', fontSize: '13px' }}>Error: {error}</p>}

      {pos && pos.length === 0 && (
        <p style={{ color: '#6b7280', fontSize: '13px' }}>No purchase orders active. Accept a reorder recommendation to generate a purchase order.</p>
      )}

      {pos && pos.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid #dcdfe4', color: '#6b7280' }}>
              <th style={{ padding: '8px 4px' }}>PO ID</th>
              <th>Product ID</th>
              <th>Qty Ordered</th>
              <th>Supplier</th>
              <th>Status</th>
              <th>Created At</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {pos.map(po => (
              <tr key={po.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                <td style={{ padding: '10px 4px', fontWeight: '600' }}>PO-{po.id}</td>
                <td>{po.productId}</td>
                <td style={{ fontWeight: '600', color: '#1a6b5a' }}>+{po.quantityOrdered} units</td>
                <td>{po.supplierId || 'SUPPLIER-DEFAULT'}</td>
                <td>
                  <span style={{
                    display: 'inline-block',
                    padding: '2px 8px',
                    borderRadius: '3px',
                    fontSize: '11px',
                    fontWeight: '600',
                    background: po.status === 'RECEIVED' ? '#dcfce7' : '#fef3c7',
                    color: po.status === 'RECEIVED' ? '#166534' : '#92400e'
                  }}>
                    {po.status}
                  </span>
                </td>
                <td style={{ color: '#6b7280', fontSize: '12px' }}>
                  {new Date(po.createdAt).toLocaleString()}
                </td>
                <td>
                  {po.status !== 'RECEIVED' ? (
                    <button
                      onClick={() => handleReceive(po.id)}
                      disabled={receivingId === po.id}
                      style={{
                        background: '#1a6b5a',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '4px',
                        padding: '4px 10px',
                        fontSize: '12px',
                        fontWeight: '500',
                        cursor: receivingId === po.id ? 'not-allowed' : 'pointer'
                      }}
                    >
                      {receivingId === po.id ? 'Processing…' : 'Receive Shipment'}
                    </button>
                  ) : (
                    <span style={{ color: '#166534', fontSize: '12px' }}>✓ Stock Ingested</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
