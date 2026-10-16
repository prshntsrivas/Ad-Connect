-- One schema per service (database-per-service pattern), all owned by the app user.
CREATE DATABASE IF NOT EXISTS adconnect_auth;
CREATE DATABASE IF NOT EXISTS adconnect_campaign;
CREATE DATABASE IF NOT EXISTS adconnect_payment;

GRANT ALL PRIVILEGES ON adconnect_auth.* TO 'adconnect'@'%';
GRANT ALL PRIVILEGES ON adconnect_campaign.* TO 'adconnect'@'%';
GRANT ALL PRIVILEGES ON adconnect_payment.* TO 'adconnect'@'%';
FLUSH PRIVILEGES;
