-- Tariff data from docs/spec/seed-data.md §1

INSERT INTO tariff_zones (id, code, name, factor) OVERRIDING SYSTEM VALUE VALUES
    (1, 'ZONA_1', 'CABA', 1.2000),
    (2, 'ZONA_2', 'Gran Buenos Aires', 1.1500),
    (3, 'ZONA_3', 'La Plata', 1.1000),
    (4, 'ZONA_4', 'Córdoba Capital', 1.0500),
    (5, 'ZONA_5', 'Rosario', 1.1000),
    (6, 'ZONA_6', 'Interior de Buenos Aires', 1.0000),
    (7, 'ZONA_7', 'Interior del país', 0.9500);

INSERT INTO provinces (id, name) OVERRIDING SYSTEM VALUE VALUES
    (1, 'CABA'),
    (2, 'Buenos Aires'),
    (3, 'Córdoba'),
    (4, 'Santa Fe'),
    (5, 'Mendoza');

INSERT INTO cities (id, province_id, name, zone_id) OVERRIDING SYSTEM VALUE VALUES
    (1, 1, 'CABA', 1),
    (2, 2, 'Quilmes', 2),
    (3, 2, 'La Matanza', 2),
    (4, 2, 'San Isidro', 2),
    (5, 2, 'La Plata', 3),
    (6, 2, 'Mar del Plata', 6),
    (7, 3, 'Córdoba', 4),
    (8, 3, 'Villa María', 7),
    (9, 4, 'Rosario', 5),
    (10, 5, 'Mendoza', 7);

INSERT INTO insurance_plans (id, code, name, description, monthly_rate, display_order) OVERRIDING SYSTEM VALUE VALUES
    (1, 'RC', 'RC — Responsabilidad civil', 'Daños a terceros', 0.0010, 1),
    (2, 'TERCEROS_COMPLETO', 'Terceros completo',
        'RC + robo e incendio totales y parciales + destrucción total + granizo + cristales', 0.0025, 2),
    (3, 'TODO_RIESGO', 'Todo riesgo', 'Terceros completo + daños parciales al propio vehículo', 0.0045, 3);

INSERT INTO usage_factors (usage_type, factor) VALUES
    ('PARTICULAR', 1.0000),
    ('COMERCIAL', 1.1000),
    ('TAXI_REMIS', 1.4000),
    ('TRANSPORTE_CARGA', 1.2500),
    ('TRANSPORTE_PASAJEROS', 1.3500),
    ('AGRICOLA', 1.0500),
    ('DELIVERY', 1.3000);

INSERT INTO allowed_usages (vehicle_type, usage_type) VALUES
    ('AUTO', 'PARTICULAR'),
    ('AUTO', 'COMERCIAL'),
    ('AUTO', 'TAXI_REMIS'),
    ('AUTO', 'DELIVERY'),
    ('MOTO', 'PARTICULAR'),
    ('MOTO', 'COMERCIAL'),
    ('MOTO', 'DELIVERY'),
    ('CAMIONETA', 'PARTICULAR'),
    ('CAMIONETA', 'COMERCIAL'),
    ('CAMIONETA', 'TRANSPORTE_CARGA'),
    ('CAMIONETA', 'AGRICOLA'),
    ('CAMIONETA', 'DELIVERY'),
    ('CAMION', 'PARTICULAR'),
    ('CAMION', 'COMERCIAL'),
    ('CAMION', 'TRANSPORTE_CARGA'),
    ('CAMION', 'TRANSPORTE_PASAJEROS'),
    ('CAMION', 'AGRICOLA');

INSERT INTO vehicle_age_factors (id, min_years, max_years, factor) OVERRIDING SYSTEM VALUE VALUES
    (1, 0, 2, 1.0000),
    (2, 3, 5, 1.0500),
    (3, 6, 10, 1.1000),
    (4, 11, NULL, 1.2000);

INSERT INTO driver_age_factors (id, min_age, max_age, factor) OVERRIDING SYSTEM VALUE VALUES
    (1, 18, 24, 1.3000),
    (2, 25, 29, 1.1500),
    (3, 30, 59, 1.0000),
    (4, 60, 69, 1.0500),
    (5, 70, NULL, 1.1500);

-- Reset identity sequences so the next id follows the last inserted one
SELECT setval(pg_get_serial_sequence('tariff_zones', 'id'), (SELECT MAX(id) FROM tariff_zones));
SELECT setval(pg_get_serial_sequence('provinces', 'id'), (SELECT MAX(id) FROM provinces));
SELECT setval(pg_get_serial_sequence('cities', 'id'), (SELECT MAX(id) FROM cities));
SELECT setval(pg_get_serial_sequence('insurance_plans', 'id'), (SELECT MAX(id) FROM insurance_plans));
SELECT setval(pg_get_serial_sequence('vehicle_age_factors', 'id'), (SELECT MAX(id) FROM vehicle_age_factors));
SELECT setval(pg_get_serial_sequence('driver_age_factors', 'id'), (SELECT MAX(id) FROM driver_age_factors));
