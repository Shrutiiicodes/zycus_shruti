import { useState, useRef, useCallback } from 'react';
import ProductList from './components/ProductList.jsx';
import SuggestionBoard from './components/SuggestionBoard.jsx';
import PurchaseOrderList from './components/PurchaseOrderList.jsx';
import AuditConsole from './components/AuditConsole.jsx';

export default function App() {
  const [activeTab, setActiveTab] = useState('console');
  const productRefreshRef = useRef(() => {});

  const registerRefresh = useCallback((fn) => {
    productRefreshRef.current = fn;
  }, []);

  const refreshProducts = useCallback(() => {
    productRefreshRef.current();
  }, []);

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', margin: 0, color: '#1a1814' }}>
            StockPulse <span style={{ fontSize: '13px', fontWeight: '500', color: '#c45a2a', background: 'rgba(196,90,42,0.1)', padding: '2px 8px', borderRadius: '4px' }}>Production Engine</span>
          </h1>
          <p style={{ color: '#6b7280', fontSize: '13px', margin: '4px 0 0 0' }}>AI Inventory & Dynamic Pricing Execution Platform</p>
        </div>
      </header>

      {/* Primary Navigation Tabs */}
      <nav style={{ display: 'flex', gap: '8px', borderBottom: '2px solid #e5e7eb', marginBottom: '28px' }}>
        <button
          onClick={() => setActiveTab('console')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'console' ? '3px solid #c45a2a' : '3px solid transparent',
            color: activeTab === 'console' ? '#c45a2a' : '#6b7280',
            fontWeight: '600',
            fontSize: '13px',
            padding: '8px 16px 12px',
            cursor: 'pointer',
            marginBottom: '-2px'
          }}
        >
          Merchandising Console
        </button>
        <button
          onClick={() => setActiveTab('purchaseOrders')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'purchaseOrders' ? '3px solid #1a6b5a' : '3px solid transparent',
            color: activeTab === 'purchaseOrders' ? '#1a6b5a' : '#6b7280',
            fontWeight: '600',
            fontSize: '13px',
            padding: '8px 16px 12px',
            cursor: 'pointer',
            marginBottom: '-2px'
          }}
        >
          Purchase Orders & Fulfillment
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'audit' ? '3px solid #b07d1a' : '3px solid transparent',
            color: activeTab === 'audit' ? '#b07d1a' : '#6b7280',
            fontWeight: '600',
            fontSize: '13px',
            padding: '8px 16px 12px',
            cursor: 'pointer',
            marginBottom: '-2px'
          }}
        >
          Audit Trails & History
        </button>
      </nav>

      {/* Tab Panels */}
      {activeTab === 'console' && (
        <>
          <div style={{ marginBottom: '32px' }}>
            <ProductList registerRefresh={registerRefresh} />
          </div>
          <SuggestionBoard onProductsChanged={refreshProducts} />
        </>
      )}

      {activeTab === 'purchaseOrders' && (
        <PurchaseOrderList />
      )}

      {activeTab === 'audit' && (
        <AuditConsole />
      )}
    </div>
  );
}