const BASE_URL = 'http://localhost:8080';

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  listProducts: () => request('/products'),
  createProduct: (data) =>
    request('/products', { method: 'POST', body: JSON.stringify(data) }),
  placeOrder: (id, quantity = 1) =>
    request(`/products/${id}/orders`, { method: 'POST', body: JSON.stringify({ quantity }) }),
  updateStock: (id, stockLevel) =>
    request(`/products/${id}/stock`, { method: 'PATCH', body: JSON.stringify({ stockLevel }) }),

  listPendingPricing: () => request('/pricing-suggestions?status=PENDING'),
  listPendingReorder: () => request('/reorder-suggestions?status=PENDING'),

  decidePricing: (id, decision) =>
    request(`/pricing-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ decision }) }),
  decideReorder: (id, decision) =>
    request(`/reorder-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ decision }) }),

  // Purchase Order & Fulfillment Endpoints
  listPurchaseOrders: (productId = '') =>
    request(`/purchase-orders${productId ? `?productId=${productId}` : ''}`),
  receiveShipment: (id) =>
    request(`/purchase-orders/${id}/receive`, { method: 'PATCH' }),

  // Audit Logs Endpoints
  getPriceHistory: (productId = '') =>
    request(`/audit/price-history${productId ? `?productId=${productId}` : ''}`),
  getInventoryTransactions: (productId = '') =>
    request(`/audit/inventory-transactions${productId ? `?productId=${productId}` : ''}`),
  getRecommendationAudits: (productId = '') =>
    request(`/audit/recommendations${productId ? `?productId=${productId}` : ''}`)
};