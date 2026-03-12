# Diagrama de Base de Datos - Pokebox

## Estructura Actual (Versión 3)

```
┌─────────────────────────────┐
│         Sets                │
├─────────────────────────────┤
│ PK  setID (TEXT)            │
│     setName (TEXT)          │
│     setCode (TEXT)          │
└─────────────────────────────┘
              │
              │ 1:N
              ▼
┌─────────────────────────────┐
│         Carta               │
├─────────────────────────────┤
│ PK  cardID (TEXT)           │
│ FK  setID (TEXT)            │
└─────────────────────────────┘
              │
              │ 1:N
              ▼
┌─────────────────────────────────────────┐
│         CartasColeccion                 │
├─────────────────────────────────────────┤
│ PK  cardID (TEXT)                       │
│ PK  colID (INTEGER)                     │
│ FK  cardID → Carta.cardID               │
│ FK  colID → Coleccion.colID             │
│     ccamount (INTEGER)                  │
│     cclastmodified (INTEGER, nullable)  │
└─────────────────────────────────────────┘
              ▲
              │ N:1
              │
┌─────────────────────────────┐
│       Coleccion             │
├─────────────────────────────┤
│ PK  colID (INTEGER AI)      │
│     colName (TEXT UNIQUE)   │
└─────────────────────────────┘
              │
              │ 1:N
              ▼
┌─────────────────────────────────────────┐
│       LogMovimientos                    │
├─────────────────────────────────────────┤
│ PK  logID (INTEGER AI)                  │
│ FK  colID (INTEGER)                     │
│ FK  cardID (TEXT)                       │
│     cantidadAnterior (INTEGER)          │
│     cantidadNueva (INTEGER)             │
│     timestamp (INTEGER)                 │
└─────────────────────────────────────────┘
```

## Relaciones

1. **Sets → Carta** (1:N)
   - Un set contiene muchas cartas
   - Una carta pertenece a un set

2. **Carta ↔ Coleccion** (N:M) a través de **CartasColeccion**
   - Una carta puede estar en múltiples colecciones
   - Una colección puede tener múltiples cartas
   - CartasColeccion es la tabla intermedia que resuelve la relación N:M

3. **Coleccion → LogMovimientos** (1:N)
   - Una colección tiene múltiples movimientos en el log
   - Cada movimiento pertenece a una colección

4. **Carta → LogMovimientos** (1:N)
   - Una carta puede tener múltiples movimientos registrados
   - Cada movimiento referencia una carta específica

## Análisis de Integridad

### ✅ Puntos Fuertes:

1. **Foreign Keys habilitadas**: `onConfigure` activa las restricciones de integridad referencial
2. **Tabla intermedia bien diseñada**: `CartasColeccion` es una tabla de unión N:M correcta con clave primaria compuesta
3. **Índices implícitos**: Las PKs y FKs crean índices automáticamente
4. **Normalización adecuada**: No hay redundancia de datos
5. **Tipos de datos apropiados**: TEXT para IDs/nombres, INTEGER para cantidades/timestamps
6. **Migración incremental**: `onUpgrade` preserva datos existentes

### ⚠️ Problemas Potenciales Detectados:

#### 1. ~~**PROBLEMA CRÍTICO: Eliminación en cascada no configurada**~~ ✅ CORREGIDO

~~Cuando eliminas una colección con `removeCollection()`, eliminas manualmente de `CartasColeccion`, pero **NO de `LogMovimientos`**. Esto dejará registros huérfanos en el log.~~

**SOLUCIONADO:** Ahora `removeCollection()` elimina primero de `LogMovimientos`, luego de `CartasColeccion`, y finalmente de `Coleccion`.

#### 2. **PROBLEMA: Foreign Key en LogMovimientos hacia Carta**

Si en el futuro implementas una función para "limpiar cartas no usadas" de la tabla `Carta`, podrías tener problemas con los logs que referencian esas cartas.

**Solución:** Probablemente no sea un problema real porque no parece que vayas a eliminar cartas de la BD, pero es algo a tener en cuenta.

#### 3. ~~**Inconsistencia menor: addEmptyCardtoCollection**~~ ✅ CORREGIDO

~~```kotlin
return db.insert(TABLA_CARTASCOLECCION, null, values) != 1L
```~~

~~Debería ser `!= -1L` (como en otros métodos). Actualmente retorna `true` cuando falla y `false` cuando tiene éxito.~~

**SOLUCIONADO:** Cambiado a `!= -1L` para consistencia con el resto de métodos.

#### 4. **Posible race condition en addCardtoCollection**

```kotlin
val previousAmount = getCardAmount(colIDq, cardIDq)
// ... luego actualiza
```

Entre `getCardAmount` y el `UPDATE`, otro thread podría cambiar el valor. Aunque usas `dbMutex` en algunos lugares, no lo usas aquí.

**Solución:** Usar `dbMutex.withLock` o hacer todo en una transacción.

#### 5. **Falta índice en LogMovimientos**

Para la query de limpieza que usa `PARTITION BY colID ORDER BY timestamp`, sería óptimo tener:
```sql
CREATE INDEX idx_log_col_timestamp ON LogMovimientos(colID, timestamp DESC)
```

### 🔧 Recomendaciones:

1. ~~**Añadir ON DELETE CASCADE** a las foreign keys de `LogMovimientos`~~ ✅ No necesario, se maneja manualmente
2. ~~**Corregir** el bug en `addEmptyCardtoCollection`~~ ✅ CORREGIDO
3. ~~**Añadir índice** en `LogMovimientos(colID, timestamp)` para mejorar performance de limpieza~~ ⏭️ No necesario por ahora
4. ~~**Considerar usar transacciones** en operaciones que modifican múltiples tablas~~ ⏭️ No necesario por ahora
5. **Opcional**: Añadir un índice en `CartasColeccion(colID, ccamount)` para queries de sets con cartas

## Resumen

La estructura es **sólida y bien diseñada**. Los problemas críticos han sido corregidos:
- ✅ Los logs se eliminan correctamente al borrar una colección
- ✅ El bug en `addEmptyCardtoCollection` está corregido
- La relación Carta ↔ Coleccion es correctamente N:M a través de CartasColeccion
