USE QuickDeliveryDB;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM package_reservation;
DELETE FROM package;
DELETE FROM vehicle;
DELETE FROM document;
DELETE FROM payment;
DELETE FROM address;
DELETE FROM user;

SET FOREIGN_KEY_CHECKS = 1;
