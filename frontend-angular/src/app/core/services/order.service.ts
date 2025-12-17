import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Order, OrderRequest, DeliveryRequest, UnifiedOrder, OrderType, PaymentStatus } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly apiUrl = `${environment.apiUrl}/admin/orders`;
  private readonly employeeApiUrl = `${environment.apiUrl}/employee/orders`;
  private readonly deliveryApiUrl = `${environment.apiUrl}/employee/deliveries`;
  private readonly unifiedApiUrl = `${environment.apiUrl}/employee/unified-orders`;
  private readonly adminUnifiedApiUrl = `${environment.apiUrl}/admin/unified-orders`;
  private readonly clientApiUrl = `${environment.apiUrl}/client/orders`;
  private readonly clientDeliveryApiUrl = `${environment.apiUrl}/client/deliveries`;

  constructor(private http: HttpClient) {}

  // Métodos unificados para empleados
  getAllUnifiedOrdersForEmployee(type?: OrderType | 'all'): Observable<UnifiedOrder[]> {
    let params = new HttpParams();
    if (type && type !== 'all') {
      params = params.set('type', type);
    }
    return this.http.get<any[]>(this.unifiedApiUrl, { params }).pipe(
      map(orders => orders.map(this.mapToUnifiedOrder))
    );
  }

  // Métodos unificados para administradores
  getAllUnifiedOrdersForAdmin(type?: OrderType | 'all'): Observable<UnifiedOrder[]> {
    let params = new HttpParams();
    if (type && type !== 'all') {
      params = params.set('type', type);
    }
    return this.http.get<any[]>(this.adminUnifiedApiUrl, { params }).pipe(
      map(orders => orders.map(this.mapToUnifiedOrder))
    );
  }

  private mapToUnifiedOrder(item: any): UnifiedOrder {
    // Asegurar que el tipo sea correcto
    let orderType: OrderType;
    if (item.type === 'EN_MESA' || item.type === OrderType.EN_MESA) {
      orderType = OrderType.EN_MESA;
    } else if (item.type === 'DOMICILIO' || item.type === OrderType.DOMICILIO) {
      orderType = OrderType.DOMICILIO;
    } else {
      // Si no tiene tipo, inferir por los campos disponibles
      if (item.tableNumber !== null && item.tableNumber !== undefined) {
        orderType = OrderType.EN_MESA;
      } else if (item.deliveryAddress || item.deliveryPhone) {
        orderType = OrderType.DOMICILIO;
      } else {
        // Default a EN_MESA si no podemos determinar
        console.warn('No se pudo determinar el tipo de pedido para item:', item);
        orderType = OrderType.EN_MESA;
      }
    }
    
    return {
      id: item.id,
      date: item.date,
      time: item.time,
      totalPrice: item.totalPrice,
      status: item.status,
      type: orderType, // Usar el tipo validado
      tableNumber: item.tableNumber, // Solo para EN_MESA
      deliveryAddress: item.deliveryAddress, // Solo para DOMICILIO
      deliveryPhone: item.deliveryPhone, // Solo para DOMICILIO
      userId: item.userId,
      userName: item.userName,
      userEmail: item.userEmail,
      items: item.items || [],
      // Campos de pago - mapear directamente desde el backend
      // Debug: log para verificar los datos que llegan
      paymentStatus: item.paymentStatus,
      paymentMethod: item.paymentMethod,
      paymentProofUrl: item.paymentProofUrl,
      verifiedBy: item.verifiedBy,
      verifiedByName: item.verifiedByName,
      verifiedAt: item.verifiedAt
    };
  }

  updateUnifiedOrderStatus(id: number, type: OrderType, status: boolean): Observable<any> {
    if (type === OrderType.EN_MESA) {
      return this.updateOrderStatus(id, status);
    } else {
      return this.updateDeliveryStatus(id, status);
    }
  }

  // Métodos para administradores confirmar pedidos unificados
  updateUnifiedOrderStatusAsAdmin(id: number, type: OrderType, status: boolean): Observable<any> {
    if (type === OrderType.EN_MESA) {
      return this.updateOrderStatusAsAdmin(id, status);
    } else {
      return this.updateDeliveryStatusAsAdmin(id, status);
    }
  }

  updateOrderStatusAsAdmin(id: number, status: boolean): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}/status`, { status });
  }

  updateDeliveryStatusAsAdmin(id: number, status: boolean): Observable<Order> {
    const adminDeliveryApiUrl = `${environment.apiUrl}/admin/deliveries`;
    return this.http.put<Order>(`${adminDeliveryApiUrl}/${id}/status`, { status });
  }

  // Métodos para pedidos en mesa (empleados)
  getAllOrdersForEmployee(): Observable<Order[]> {
    return this.http.get<Order[]>(this.employeeApiUrl);
  }

  getOrderByIdForEmployee(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.employeeApiUrl}/${id}`);
  }

  createOrderAsEmployee(order: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.employeeApiUrl, order);
  }

  updateOrderStatus(id: number, status: boolean): Observable<Order> {
    return this.http.put<Order>(`${this.employeeApiUrl}/${id}/status`, { status });
  }

  // Métodos para pedidos a domicilio (empleados)
  getAllDeliveriesForEmployee(): Observable<Order[]> {
    return this.http.get<Order[]>(this.deliveryApiUrl);
  }

  getDeliveryByIdForEmployee(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.deliveryApiUrl}/${id}`);
  }

  createDeliveryAsEmployee(delivery: DeliveryRequest): Observable<Order> {
    return this.http.post<Order>(this.deliveryApiUrl, delivery);
  }

  updateDeliveryStatus(id: number, status: boolean): Observable<Order> {
    return this.http.put<Order>(`${this.deliveryApiUrl}/${id}/status`, { status });
  }

  // Métodos para administradores
  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  getOrderById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  getOrdersByDate(date: string): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/date/${date}`);
  }

  // Métodos para clientes
  createOrder(order: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.clientApiUrl, order);
  }

  createDelivery(delivery: DeliveryRequest): Observable<Order> {
    return this.http.post<Order>(this.clientDeliveryApiUrl, delivery);
  }

  getClientOrders(userId?: number): Observable<Order[]> {
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId.toString());
    }
    return this.http.get<Order[]>(this.clientApiUrl, { params });
  }

  getClientOrderById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.clientApiUrl}/${id}`);
  }

  getClientDeliveries(userId?: number): Observable<Order[]> {
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId.toString());
    }
    return this.http.get<Order[]>(this.clientDeliveryApiUrl, { params });
  }

  getClientDeliveryById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.clientDeliveryApiUrl}/${id}`);
  }

  // Métodos para pagos de orders
  getOrdersWithPendingPayments(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/pending-payments`);
  }

  getOrdersWithVerifiedPayments(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/verified-payments`);
  }

  getDeliveriesWithVerifiedPayments(): Observable<Order[]> {
    const adminDeliveriesApiUrl = `${environment.apiUrl}/admin/deliveries`;
    return this.http.get<any[]>(`${adminDeliveriesApiUrl}/verified-payments`).pipe(
      map(deliveries => deliveries.map(item => this.mapDeliveryToOrder(item)))
    );
  }

  private mapDeliveryToOrder(delivery: any): Order {
    return {
      id: delivery.id,
      date: delivery.date,
      time: delivery.time,
      totalPrice: delivery.totalPrice,
      status: delivery.status,
      type: OrderType.DOMICILIO,
      deliveryAddress: delivery.deliveryAddress,
      deliveryPhone: delivery.deliveryPhone,
      userId: delivery.userId,
      userName: delivery.userName,
      userEmail: delivery.userEmail,
      items: delivery.items || [],
      // Campos de pago - mapear de DeliveryResponse a Order (el backend usa verifiedBy/verifiedByName en el Map unificado)
      paymentStatus: delivery.paymentStatus,
      paymentMethod: delivery.paymentMethod,
      paymentProofUrl: delivery.paymentProofUrl,
      verifiedBy: delivery.verifiedBy || delivery.verifiedByAdminId,
      verifiedByName: delivery.verifiedByName || delivery.verifiedByAdminName,
      verifiedAt: delivery.verifiedAt
    };
  }

  verifyPayment(orderId: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/verify-payment`, {});
  }

  rejectPayment(orderId: number, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/reject-payment`, { reason });
  }

  // Métodos para pagos de deliveries
  getDeliveriesWithPendingPayments(): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/admin/deliveries/pending-payments`);
  }

  verifyDeliveryPayment(deliveryId: number): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/admin/deliveries/${deliveryId}/verify-payment`, {});
  }

  rejectDeliveryPayment(deliveryId: number): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/admin/deliveries/${deliveryId}/reject-payment`, {});
  }

  uploadPaymentProof(orderId: number, file: File): Observable<Order> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.clientApiUrl}/${orderId}/upload-payment-proof`, formData).pipe(
      map(response => {
        // El backend puede devolver { order: OrderResponse } o directamente OrderResponse
        return response.order || response;
      })
    );
  }

  // Métodos para pagos de deliveries
  uploadDeliveryPaymentProof(deliveryId: number, file: File): Observable<Order> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.clientDeliveryApiUrl}/${deliveryId}/upload-payment-proof`, formData).pipe(
      map(response => {
        // El backend puede devolver { order: DeliveryResponse } o directamente DeliveryResponse
        return response.order || response;
      })
    );
  }
}

