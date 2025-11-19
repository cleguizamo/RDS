import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Order, OrderRequest, DeliveryRequest, UnifiedOrder, OrderType } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly apiUrl = `${environment.apiUrl}/admin/orders`;
  private readonly employeeApiUrl = `${environment.apiUrl}/employee/orders`;
  private readonly deliveryApiUrl = `${environment.apiUrl}/employee/deliveries`;
  private readonly unifiedApiUrl = `${environment.apiUrl}/employee/unified-orders`;
  private readonly adminUnifiedApiUrl = `${environment.apiUrl}/admin/unified-orders`;

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
      items: item.items || []
    };
  }

  updateUnifiedOrderStatus(id: number, type: OrderType, status: boolean): Observable<any> {
    if (type === OrderType.EN_MESA) {
      return this.updateOrderStatus(id, status);
    } else {
      return this.updateDeliveryStatus(id, status);
    }
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
}

