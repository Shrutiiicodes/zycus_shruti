import { useRef, useCallback } from 'react';
import ProductList from './components/ProductList.jsx';
import SuggestionBoard from './components/SuggestionBoard.jsx';

export default function App() {
  const productRefreshRef = useRef(() => {});

  const registerRefresh = useCallback((fn) => {
    productRefreshRef.current = fn;
  }, []);

  const refreshProducts = useCallback(() => {
    productRefreshRef.current();
  }, []);

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto', padding: '32px 20px', fontFamily: 'system-ui, sans-serif' }}>
      <h1 style={{ fontSize: '22px', marginBottom: '4px' }}>StockPulse</h1>
      <p style={{ color: '#6b7280', fontSize: '13px', marginBottom: '28px' }}>Merchandising console</p>

      <div style={{ marginBottom: '32px' }}>
        <ProductList registerRefresh={registerRefresh} />
      </div>

      <SuggestionBoard onProductsChanged={refreshProducts} />
    </div>
  );
}