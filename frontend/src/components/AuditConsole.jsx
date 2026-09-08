import { useState } from 'react';
import { usePolling } from '../hooks/usePolling.js';
import { api } from '../api/client.js';

export default function AuditConsole() {
  const [activeSubTab, setActiveSubTab] = useState('priceHistory');

  const { data: priceHistory } = usePolling(api.getPriceHistory, 5000);
  const { data: invTx } = usePolling(api.getInventoryTransactions, 5000);
  const { data: recAudits } = usePolling(api.getRecommendationAudits, 5000);

  return (
    <div style={{ background: '#ffffff', border: '1px solid #dcdfe4', borderRadius: '6px', padding: '20px' }}>
      <div style={{ display: 'flex', gap: '12px', borderBottom: '1px solid #e5e7eb', paddingBottom: '12px', marginBottom: '16px' }}>
        <button
          onClick={() => setActiveSubTab('priceHistory')}
          style={{
            background: activeSubTab === 'priceHistory' ? '#c45a2a' : 'transparent',
            color: activeSubTab === 'priceHistory' ? '#ffffff' : '#4b5563',
            border: 'none',
            borderRadius: '4px',
            padding: '6px 14px',
            fontSize: '12px',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          Price History ({priceHistory ? priceHistory.length : 0})
        </button>
        <button
          onClick={() => setActiveSubTab('inventoryTx')}
          style={{
            background: activeSubTab === 'inventoryTx' ? '#1a6b5a' : 'transparent',
            color: activeSubTab === 'inventoryTx' ? '#ffffff' : '#4b5563',
            border: 'none',
            borderRadius: '4px',
            padding: '6px 14px',
            fontSize: '12px',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          Inventory Transactions ({invTx ? invTx.length : 0})
        </button>
        <button
          onClick={() => setActiveSubTab('recAudits')}
          style={{
            background: activeSubTab === 'recAudits' ? '#b07d1a' : 'transparent',
            color: activeSubTab === 'recAudits' ? '#ffffff' : '#4b5563',
            border: 'none',
            borderRadius: '4px',
            padding: '6px 14px',
            fontSize: '12px',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          Recommendation Audits ({recAudits ? recAudits.length : 0})
        </button>
      </div>

      {activeSubTab === 'priceHistory' && (
        <div>
          <h3 style={{ fontSize: '14px', marginBottom: '10px', color: '#1a1814' }}>Price History Log</h3>
          {(!priceHistory || priceHistory.length === 0) && <p style={{ color: '#6b7280', fontSize: '13px' }}>No price changes recorded yet.</p>}
          {priceHistory && priceHistory.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
              <thead>
                <tr style={{ textAlign: 'left', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                  <th style={{ padding: '6px' }}>Timestamp</th>
                  <th>Product</th>
                  <th>Old Price</th>
                  <th>New Price</th>
                  <th>Actor</th>
                  <th>Reason</th>
                </tr>
              </thead>
              <tbody>
                {priceHistory.map(ph => (
                  <tr key={ph.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '6px', color: '#6b7280' }}>{new Date(ph.timestamp).toLocaleString()}</td>
                    <td style={{ fontWeight: '600' }}>{ph.productId}</td>
                    <td>${ph.oldPrice.toFixed(2)}</td>
                    <td style={{ color: '#c45a2a', fontWeight: '600' }}>${ph.newPrice.toFixed(2)}</td>
                    <td>{ph.changedBy}</td>
                    <td style={{ color: '#4b5563' }}>{ph.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeSubTab === 'inventoryTx' && (
        <div>
          <h3 style={{ fontSize: '14px', marginBottom: '10px', color: '#1a1814' }}>Inventory Transaction Log</h3>
          {(!invTx || invTx.length === 0) && <p style={{ color: '#6b7280', fontSize: '13px' }}>No inventory movements recorded yet.</p>}
          {invTx && invTx.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
              <thead>
                <tr style={{ textAlign: 'left', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                  <th style={{ padding: '6px' }}>Timestamp</th>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Delta</th>
                  <th>New Stock</th>
                  <th>Ref ID</th>
                </tr>
              </thead>
              <tbody>
                {invTx.map(tx => (
                  <tr key={tx.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '6px', color: '#6b7280' }}>{new Date(tx.timestamp).toLocaleString()}</td>
                    <td style={{ fontWeight: '600' }}>{tx.productId}</td>
                    <td><span style={{ fontWeight: '600', color: tx.transactionType === 'SALE' ? '#dc2626' : '#166534' }}>{tx.transactionType}</span></td>
                    <td style={{ fontWeight: '600', color: tx.quantityDelta > 0 ? '#166534' : '#dc2626' }}>
                      {tx.quantityDelta > 0 ? `+${tx.quantityDelta}` : tx.quantityDelta}
                    </td>
                    <td style={{ fontWeight: '600' }}>{tx.newStockLevel}</td>
                    <td style={{ color: '#6b7280' }}>{tx.referenceId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeSubTab === 'recAudits' && (
        <div>
          <h3 style={{ fontSize: '14px', marginBottom: '10px', color: '#1a1814' }}>Recommendation Decision Audits</h3>
          {(!recAudits || recAudits.length === 0) && <p style={{ color: '#6b7280', fontSize: '13px' }}>No recommendation audits recorded yet.</p>}
          {recAudits && recAudits.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
              <thead>
                <tr style={{ textAlign: 'left', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                  <th style={{ padding: '6px' }}>Timestamp</th>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Trigger</th>
                  <th>Output Value</th>
                  <th>Confidence</th>
                  <th>Status</th>
                  <th>Decided By</th>
                </tr>
              </thead>
              <tbody>
                {recAudits.map(ra => (
                  <tr key={ra.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '6px', color: '#6b7280' }}>{new Date(ra.timestamp).toLocaleString()}</td>
                    <td style={{ fontWeight: '600' }}>{ra.productId}</td>
                    <td>{ra.suggestionType}</td>
                    <td>{ra.triggerReason}</td>
                    <td style={{ fontWeight: '600' }}>
                      {ra.calculatedPrice ? `$${ra.calculatedPrice.toFixed(2)}` : `${ra.calculatedQuantity} units`}
                    </td>
                    <td>{(ra.systemConfidence * 100).toFixed(0)}%</td>
                    <td>
                      <span style={{
                        padding: '2px 6px',
                        borderRadius: '3px',
                        fontWeight: '600',
                        fontSize: '10px',
                        background: ra.status === 'ACCEPTED' ? '#dcfce7' : '#fee2e2',
                        color: ra.status === 'ACCEPTED' ? '#166534' : '#991b1b'
                      }}>
                        {ra.status}
                      </span>
                    </td>
                    <td>{ra.decidedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
