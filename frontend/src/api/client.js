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
  placeOrder: (id, quantity = 1) =>
    request(`/products/${id}/orders`, { method: 'POST', body: JSON.stringify({ quantity }) }),
  updateStock: (id, stockLevel) =>
    request(`/products/${id}/stock`, { method: 'PATCH', body: JSON.stringify({ stockLevel }) }),

  listPendingPricing: () => request('/pricing-suggestions?status=PENDING'),
  listPendingReorder: () => request('/reorder-suggestions?status=PENDING'),

  decidePricing: (id, decision) =>
    request(`/pricing-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ decision }) }),
  decideReorder: (id, decision) =>
    request(`/reorder-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ decision }) })
};