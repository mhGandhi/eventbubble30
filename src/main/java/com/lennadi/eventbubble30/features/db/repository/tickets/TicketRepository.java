package com.lennadi.eventbubble30.features.db.repository.tickets;

import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByExternalIdIgnoreCase(String externalId);

    public enum OrderBy {
        creationDate,
        modificationDate
    }
    public enum OrderDir {
        asc, desc
    }

    public final class TicketSpecs {
        private TicketSpecs() {}

        public static Specification<Ticket> messageSearch(String q) {
            return (root, query, cb) -> {
                if (q == null || q.isBlank()) return null;
                String like = "%" + q.toLowerCase() + "%";
                return cb.like(cb.lower(root.get("message")), like);
            };
        }

        public static Specification<Ticket> closed(Boolean closed) {
            return (root, query, cb) -> closed == null ? null : cb.equal(root.get("closed"), closed);
        }

        public static Specification<Ticket> escalate(Boolean escalate) {
            return (root, query, cb) -> escalate == null ? null : cb.equal(root.get("escalate"), escalate);
        }

        public static Specification<Ticket> createdBy(String userExtId) {
            return (root, query, cb) -> {
                if (userExtId == null || userExtId.isBlank()) return null;
                return cb.equal(root.get("createdBy").get("externalId"), userExtId);
            };
        }

        public static Specification<Ticket> assignedTo(String userExtId) {
            return (root, query, cb) -> {
                if (userExtId == null || userExtId.isBlank()) return null;
                return cb.equal(root.get("assignedTo").get("externalId"), userExtId);
            };
        }

        public static Specification<Ticket> createdBetween(Instant from, Instant to) {
            return (root, query, cb) -> {
                if (from == null && to == null) return null;

                if (from != null && to != null)
                    return cb.between(root.get("creationDate"), from, to);

                if (from != null)
                    return cb.greaterThanOrEqualTo(root.get("creationDate"), from);

                return cb.lessThanOrEqualTo(root.get("creationDate"), to);
            };
        }

        public static Specification<Ticket> modifiedBetween(Instant from, Instant to) {
            return (root, query, cb) -> {
                if (from == null && to == null) return null;

                if (from != null && to != null)
                    return cb.between(root.get("modificationDate"), from, to);

                if (from != null)
                    return cb.greaterThanOrEqualTo(root.get("modificationDate"), from);

                return cb.lessThanOrEqualTo(root.get("modificationDate"), to);
            };
        }

        /** requires discriminator column mapped as `ticketType` on Ticket */
        public static Specification<Ticket> typeEquals(String type) {
            return (root, query, cb) -> {
                if (type == null || type.isBlank()) return null;
                return cb.equal(cb.upper(root.get("ticketType")), type.toUpperCase());
            };
        }
    }
}

