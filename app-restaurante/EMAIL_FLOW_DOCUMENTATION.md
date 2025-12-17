# Flujo de Notificaciones por Email - Sistema de Pedidos

Este documento describe el flujo completo de notificaciones por email para pedidos (orders) y entregas (deliveries).

**📍 Ubicación Centralizada:** Todos los emails se gestionan desde `EmailService.java`

## Flujo General

### 1. Creación de Pedido/Entrega (Estado: PENDING)

**Cuando se crea:**
- Se envía email de "Recibido" (no confirmado)
- El estado del pedido es `status = false` (pendiente)
- El estado de pago es `PaymentStatus.PENDING`

**Email enviado:**
- `OrderService.createOrder()` → `emailService.sendOrderReceivedEmail()`
- `DeliveryService.createDelivery()` → `emailService.sendDeliveryReceivedEmail()`

**Acciones del usuario:**
- Solo se actualiza `numberOfOrders` y `lastOrderDate` en el usuario
- **NO** se actualiza `totalSpent` ni puntos
- **NO** se registra ingreso en BalanceService

---

### 2. Verificación de Pago (Estado: VERIFIED)

**Cuando el admin verifica el pago:**
- Se envía email de "Confirmación de Pago y Pedido"
- El estado de pago cambia a `PaymentStatus.VERIFIED`
- Se actualiza `totalSpent` y puntos del usuario
- Se registra ingreso en `BalanceService`

**Email enviado:**
- `OrderService.verifyPayment()` → `emailService.sendOrderConfirmationEmail()`
- `DeliveryService.verifyPayment()` → `emailService.sendDeliveryConfirmationEmail()`

**Acciones del sistema:**
- Se actualiza `totalSpent` del usuario (suma del monto del pedido)
- Se actualizan puntos del usuario según el sistema de recompensas
- Se registra ingreso en `BalanceService.recordIncome()`
- Se guarda `verifiedBy` (admin) y `verifiedAt` (timestamp)

---

### 3. Completado del Pedido/Entrega (Estado: status = true)

**Cuando el admin marca como completado:**
- Se envía email final según el tipo de pedido
- El estado del pedido cambia a `status = true` (completado)

**Email enviado:**
- `OrderService.updateOrderStatus()` (si es EN_MESA) → `emailService.sendOrderDeliveredEmail()` 
- `DeliveryService.updateDeliveryStatus()` (si es DOMICILIO) → `emailService.sendGenericEmail()` con asunto "Tu pedido va en camino"

**Acciones del sistema:**
- Solo se actualiza el estado del pedido
- **NO** se registra ingreso adicional (ya se hizo en verificación de pago)

---

## Resumen de Estados y Emails

| Estado del Pedido | Estado de Pago | Email Enviado | Actualizaciones |
|-------------------|----------------|---------------|-----------------|
| `status = false` | `PENDING` | `sendOrderReceivedEmail()` / `sendDeliveryReceivedEmail()` | `numberOfOrders`, `lastOrderDate` |
| `status = false` | `VERIFIED` | `sendOrderConfirmationEmail()` / `sendDeliveryConfirmationEmail()` | `totalSpent`, `points`, `BalanceService.recordIncome()` |
| `status = true` | `VERIFIED` | `sendOrderDeliveredEmail()` / `sendGenericEmail()` (en camino) | Ninguna (solo cambio de estado) |

---

## Notas Importantes

1. **El orden es crítico:** Primero se verifica el pago (VERIFIED), luego se completa el pedido (status = true)
2. **Los ingresos solo se registran una vez:** Cuando el pago es verificado, no cuando se completa
3. **Los emails son informativos:** No afectan el estado del pedido, solo notifican al usuario
4. **Manejo de errores:** Si un email falla, el proceso continúa (se registra un warning en el log)

---

## Casos Especiales

### Pedidos en Mesa (EN_MESA)
- No requieren comprobante de pago (se paga en efectivo en la tienda)
- El admin puede verificar el pago directamente
- Al completarse, se envía email de "Pedido entregado en la mesa X"

### Entregas a Domicilio (DOMICILIO)
- Requieren comprobante de pago (excepto si es efectivo)
- El cliente sube el comprobante al crear el pedido
- Al completarse, se envía email de "Tu pedido va en camino"

---

## Templates de Email

- `order-received.html` - Pedido recibido (sin confirmar)
- `order-confirmation.html` - Pago verificado y pedido confirmado
- `order-delivered.html` - Pedido entregado en mesa
- `delivery-received.html` - Entrega recibida (sin confirmar)
- `delivery-confirmation.html` - Pago verificado y entrega confirmada
- (Email genérico) - Entrega en camino

