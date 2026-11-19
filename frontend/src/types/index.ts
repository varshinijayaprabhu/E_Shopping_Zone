export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  details?: string[];
  timestamp?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

export interface User {
  id: number;
  email: string;
  fullName?: string;
  phone?: string;
  roles: string[];
  active?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  active?: boolean;
}

export interface Product {
  id: number;
  merchantId: number;
  categoryId: number;
  categoryName: string;
  name: string;
  sku: string;
  description: string;
  price: number;
  imageUrl: string;
  availableStock: number;
  reservedStock?: number;
  active: boolean;
}

export interface ProductCreatePayload {
  categoryId: number;
  name: string;
  sku: string;
  description: string;
  price: number;
  imageUrl: string;
  initialStock: number;
}

export interface ProductUpdatePayload {
  categoryId: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  availableStock: number;
  active: boolean;
}

export interface CartItem {
  itemId: number;
  productId: number;
  productName: string;
  productSku: string;
  unitPrice: number;
  imageUrl: string;
  quantity: number;
  itemSubtotal: number;
  inStock: boolean;
  availableStock: number;
}

export interface Cart {
  cartId: number;
  customerId: number;
  items: CartItem[];
  totalItems: number;
  cartSubtotal: number;
}

export interface Wallet {
  walletId: number;
  customerId: number;
  currentBalance: number;
  currency: string;
  status: string;
}

export interface WalletTransaction {
  transactionId: number;
  transactionReference: string;
  orderReference?: string;
  transactionType: 'DEPOSIT' | 'CHECKOUT_PAYMENT' | 'REFUND';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  status: string;
  description?: string;
  createdAt: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productNameSnapshot: string;
  productSkuSnapshot?: string;
  unitPriceSnapshot: number;
  quantity: number;
  subtotal: number;
  merchantId: number;
}

export interface PaymentRecord {
  id: number;
  paymentReference: string;
  paymentMethod: 'WALLET' | 'CASH_ON_DELIVERY' | 'RAZORPAY';
  paymentStatus: string;
  amount: number;
  gatewayOrderId?: string;
  gatewayPaymentId?: string;
  gatewaySignature?: string;
  createdAt: string;
}

export interface RazorpayOrderIntent {
  razorpayOrderId: string;
  orderNumber: string;
  amount: number;
  amountInPaise: number;
  currency: string;
  keyId: string;
}

export interface RazorpayVerifyPayload {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  orderNumber: string;
  shippingName: string;
  shippingPhone: string;
  shippingAddress: string;
  idempotencyKey?: string;
}

export interface Order {
  id: number;
  orderNumber: string;
  customerId: number;
  orderStatus: 'PENDING' | 'PLACED' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  sagaState: string;
  subtotalAmount: number;
  shippingFee: number;
  totalAmount: number;
  shippingName: string;
  shippingPhone: string;
  shippingAddress: string;
  sagaFailureReason?: string;
  createdAt: string;
  updatedAt?: string;
  items: OrderItem[];
  payment?: PaymentRecord;
}

export interface MerchantOrderItem {
  orderItemId: number;
  orderId?: number;
  orderNumber: string;
  orderStatus: string;
  orderCreatedAt: string;
  productId: number;
  productNameSnapshot: string;
  productSkuSnapshot?: string;
  unitPriceSnapshot: number;
  quantity: number;
  subtotal: number;
  shippingName: string;
  shippingPhone: string;
  shippingAddress: string;
}

export interface CheckoutPayload {
  idempotencyKey: string;
  paymentMethod: 'WALLET' | 'CASH_ON_DELIVERY';
  shippingName: string;
  shippingPhone: string;
  shippingAddress: string;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
}

export interface DashboardStats {
  totalOrders: number;
  totalRevenue: number;
  totalUsers: number;
  activeCustomers: number;
  registeredMerchants: number;
  pendingMerchants: number;
  pendingDisputes: number;
  escalatedDisputes: number;
  resolvedDisputes: number;
}

export interface MerchantProfile {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  city?: string;
  state?: string;
  country?: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
}

export interface DisputeTicket {
  id: number;
  orderId: number;
  customerId: number;
  merchantId: number;
  issueReason: string;
  customerComment?: string;
  merchantResponse?: string;
  adminNotes?: string;
  refundAmount: number;
  status: 'PENDING_MERCHANT' | 'MERCHANT_ACCEPTED' | 'MERCHANT_REJECTED' | 'ESCALATED_TO_ADMIN' | 'REFUNDED_BY_ADMIN' | 'RESOLVED_NO_REFUND';
  merchantDeadline?: string;
  remainingSeconds?: number;
  escalatedAt?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminAuditLog {
  id: number;
  adminId: number;
  actionType: string;
  targetEntityType: string;
  targetEntityId: number;
  reason?: string;
  metadata?: string;
  ipAddress?: string;
  createdAt: string;
}

