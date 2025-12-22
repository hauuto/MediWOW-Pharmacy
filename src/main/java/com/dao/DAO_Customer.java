package com.dao;

import com.entities.Customer;
import com.interfaces.ICustomer;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * @author Tô Thanh Hậu
 */
public class DAO_Customer implements ICustomer {
    private final SessionFactory sessionFactory;

    public DAO_Customer() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public boolean addCustomer(Customer customer) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            // If id is not provided, generate one using DB sequence, but keep it in sync with existing data
            if (customer.getId() == null || customer.getId().trim().isEmpty()) {
                java.time.Year currentYear = java.time.Year.now();
                String yearStr = currentYear.toString();

                // Find max existing numeric suffix for this year (e.g., from 'CUS2025-0005' get 5)
                String pattern = "CUS" + yearStr + "-%";
                String maxIdSql = "SELECT MAX(id) FROM Customer WHERE id LIKE :pattern";
                String maxId = session.createNativeQuery(maxIdSql, String.class)
                        .setParameter("pattern", pattern)
                        .uniqueResult();
                int maxExisting = 0;
                if (maxId != null && maxId.contains("-")) {
                    String suffix = maxId.substring(maxId.lastIndexOf('-') + 1);
                    try {
                        maxExisting = Integer.parseInt(suffix);
                    } catch (NumberFormatException nfe) {
                        maxExisting = 0;
                    }
                }

                Long seqVal = null;
                boolean sequenceAvailable = true;
                try {
                    Object seqObj = session.createNativeQuery("SELECT NEXT VALUE FOR dbo.CustomerSeg").uniqueResult();
                    if (seqObj instanceof Number) {
                        seqVal = ((Number) seqObj).longValue();
                    } else if (seqObj != null) {
                        seqVal = Long.parseLong(seqObj.toString());
                    }
                } catch (Exception ex) {
                    // sequence not available or permission issue
                    sequenceAvailable = false;
                    seqVal = null;
                }

                if (sequenceAvailable && seqVal != null) {
                    if (seqVal <= maxExisting) {
                        // advance sequence to maxExisting + 1
                        long restartWith = (long) maxExisting + 1L;
                        try {
                            session.createNativeQuery("ALTER SEQUENCE dbo.CustomerSeg RESTART WITH " + restartWith).executeUpdate();
                            // fetch next value after restart
                            Object seqObj2 = session.createNativeQuery("SELECT NEXT VALUE FOR dbo.CustomerSeg").uniqueResult();
                            if (seqObj2 instanceof Number) {
                                seqVal = ((Number) seqObj2).longValue();
                            } else if (seqObj2 != null) {
                                seqVal = Long.parseLong(seqObj2.toString());
                            }
                        } catch (Exception ex) {
                            // If altering sequence fails, fallback to using maxExisting+1 without touching sequence
                            seqVal = (long) maxExisting + 1L;
                            sequenceAvailable = false; // mark as not fully synced
                        }
                    }
                } else {
                    // sequence not available: fallback to using maxExisting+1
                    seqVal = (long) maxExisting + 1L;
                }

                // Build ID and set
                String generatedId = String.format("CUS%s-%04d", yearStr, seqVal != null ? seqVal : (maxExisting + 1));
                customer.setId(generatedId);
            }

            // Persist the customer (id is set)
            session.persist(customer);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean updateCustomer(Customer customer) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.merge(customer);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean deleteCustomer(String id) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            Customer customer = session.get(Customer.class, id);
            if (customer != null) {
                session.remove(customer);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Customer> getAllCustomers() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer ORDER BY creationDate DESC",
                Customer.class
            );
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Customer getCustomerById(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.get(Customer.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Customer getCustomerByPhoneNumber(String phoneNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer WHERE phoneNumber = :phoneNumber",
                Customer.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Customer> searchCustomersByName(String name) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Customer> query = session.createQuery(
                "FROM Customer WHERE LOWER(name) LIKE :name ORDER BY name",
                Customer.class
            );
            query.setParameter("name", "%" + name.toLowerCase() + "%");
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM Customer WHERE phoneNumber = :phoneNumber",
                Long.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query<Long> query = session.createQuery(
                "SELECT COUNT(*) FROM Customer WHERE phoneNumber = :phoneNumber AND id != :excludeId",
                Long.class
            );
            query.setParameter("phoneNumber", phoneNumber);
            query.setParameter("excludeId", excludeId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Search top 5 customers by name (contains, case-insensitive) or phone (contains).
     */
    public List<Customer> searchTop5ByNameOrPhone(String keyword) {
        if (keyword == null) return java.util.Collections.emptyList();
        String kw = keyword.trim();
        if (kw.isEmpty()) return java.util.Collections.emptyList();

        Session session = null;
        try {
            session = sessionFactory.openSession();
            String jpql = "FROM Customer c WHERE lower(c.name) LIKE :kw OR c.phoneNumber LIKE :phone ORDER BY c.name";
            Query<Customer> q = session.createQuery(jpql, Customer.class);
            q.setParameter("kw", "%" + kw.toLowerCase() + "%");
            q.setParameter("phone", "%" + kw + "%");
            q.setMaxResults(5);
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

}
