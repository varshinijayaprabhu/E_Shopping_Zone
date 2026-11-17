import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { NotificationProvider } from './context/NotificationContext';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { WalletProvider } from './context/WalletContext';

import { ToastContainer } from './components/common/ToastContainer';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { RoleRoute } from './components/common/RoleRoute';

// Public & Customer Pages
import { HomePage } from './pages/HomePage';
import { ProductListPage } from './pages/ProductListPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { WalletPage } from './pages/WalletPage';
import { OrderHistoryPage } from './pages/OrderHistoryPage';
import { OrderDetailPage } from './pages/OrderDetailPage';
import { ProfilePage } from './pages/ProfilePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { OAuthRedirectHandler } from './pages/OAuthRedirectHandler';
import { NotFoundPage } from './pages/NotFoundPage';

// Merchant Pages
import { MerchantDashboardPage } from './pages/merchant/MerchantDashboardPage';
import { MerchantProductsPage } from './pages/merchant/MerchantProductsPage';
import { MerchantProductFormPage } from './pages/merchant/MerchantProductFormPage';
import { MerchantOrdersPage } from './pages/merchant/MerchantOrdersPage';

const App: React.FC = () => {
  return (
    <NotificationProvider>
      <AuthProvider>
        <CartProvider>
          <WalletProvider>
            <BrowserRouter>
              <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800 font-sans selection:bg-indigo-500 selection:text-white">
                <ToastContainer />
                <Navbar />

                <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-6">
                  <Routes>
                    {/* Public Routes */}
                    <Route path="/" element={<HomePage />} />
                    <Route path="/products" element={<ProductListPage />} />
                    <Route path="/products/:id" element={<ProductDetailPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/oauth2/redirect" element={<OAuthRedirectHandler />} />

                    {/* Customer Protected Routes */}
                    <Route
                      path="/cart"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_CUSTOMER">
                            <CartPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/checkout"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_CUSTOMER">
                            <CheckoutPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/wallet"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_CUSTOMER">
                            <WalletPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/orders"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_CUSTOMER">
                            <OrderHistoryPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/orders/:id"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_CUSTOMER">
                            <OrderDetailPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/profile"
                      element={
                        <ProtectedRoute>
                          <ProfilePage />
                        </ProtectedRoute>
                      }
                    />

                    {/* Merchant Protected Routes */}
                    <Route
                      path="/merchant"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_MERCHANT">
                            <MerchantDashboardPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route path="/merchant/dashboard" element={<Navigate to="/merchant" replace />} />
                    <Route path="/dashboard" element={<Navigate to="/merchant" replace />} />
                    <Route
                      path="/merchant/products"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_MERCHANT">
                            <MerchantProductsPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/merchant/products/new"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_MERCHANT">
                            <MerchantProductFormPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/merchant/products/edit/:id"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_MERCHANT">
                            <MerchantProductFormPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />
                    <Route
                      path="/merchant/orders"
                      element={
                        <ProtectedRoute>
                          <RoleRoute requiredRole="ROLE_MERCHANT">
                            <MerchantOrdersPage />
                          </RoleRoute>
                        </ProtectedRoute>
                      }
                    />

                    {/* 404 Catch All */}
                    <Route path="*" element={<NotFoundPage />} />
                  </Routes>
                </main>

                <Footer />
              </div>
            </BrowserRouter>
          </WalletProvider>
        </CartProvider>
      </AuthProvider>
    </NotificationProvider>
  );
};

export default App;
