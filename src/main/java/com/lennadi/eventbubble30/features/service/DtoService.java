package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.exceptions.ErrorCodes;
import com.lennadi.eventbubble30.features.DTOLevel;
import com.lennadi.eventbubble30.features.IDTO;
import com.lennadi.eventbubble30.features.db.Location;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.fileStorage.FileManagerService;
import com.lennadi.eventbubble30.logging.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class DtoService {
    private final FileManagerService fileManagerService;

    public IDTO get(Profil p){return get(p,DTOLevel.FULL);}
    public IDTO get(Profil p, DTOLevel lvl){
        if(lvl == DTOLevel.MOD){
            assertAdmin();
            return new Profil.ModDTO(
                    p.getExternalId(), p.getName(), p.hasAvatar() ? fileManagerService.getURL(p.getAvatar()) : null, p.getBio(),
                    p.getCreationDate(), p.getModificationDate()
            );
        }

        if (lvl == DTOLevel.CARD) {
            return new Profil.CardDTO(p.getExternalId(), p.getName(), p.hasAvatar() ? fileManagerService.getURL(p.getAvatar()) : null);
        }

        return new Profil.DTO(p.getExternalId(), p.getName(), p.hasAvatar() ? fileManagerService.getURL(p.getAvatar()) : null, p.getBio());
    }

    public IDTO get(Benutzer b){return get(b,DTOLevel.FULL);}
    public IDTO get(Benutzer b, DTOLevel lvl) {
        if(lvl == DTOLevel.MOD){
            assertAdmin();
            return new Benutzer.ModDTO(
                    b.getExternalId(), b.getUsername(), b.getRoles(),
                    b.getEmail(), b.isEmailVerified(),
                    b.getLastLoginDate(), b.getLastSeen(),
                    b.getPasswordChangedAt(), b.getTokensInvalidatedAt()
            );
        }

        return new Benutzer.DTO(b.getExternalId(), b.getUsername(), b.getRoles());
    }

    //todo ist arsch
    public IDTO get(Veranstaltung v, boolean bookmarked){return get(v, bookmarked,DTOLevel.FULL);}
    public IDTO get(Veranstaltung v, boolean bookmarked, DTOLevel lvl) {
        Benutzer bes = v.getBesitzer();

        if(lvl == DTOLevel.MOD){
            assertAdmin();
            return new Veranstaltung.ModDTO(
                    v.getExternalId(), v.getTitle(), v.getTermin(), bes!=null?get(bes):null, v.getLocation(), bookmarked,
                    v.getDescription(), v.getCreationDate(),
                    v.getModificationDate()
            );
        }

        if (lvl == DTOLevel.CARD) {
            return new Veranstaltung.CardDTO(
                    v.getExternalId(), v.getTitle(), v.getTermin(), bes!=null?get(bes):null, v.getLocation(), bookmarked
            );
        }

        return new Veranstaltung.DTO(
                v.getExternalId(), v.getTitle(), v.getTermin(), bes!=null?get(bes):null, v.getLocation(), bookmarked,
                v.getDescription(), v.getCreationDate()
        );
    }

    public IDTO get(AuditLog a){return get(a,DTOLevel.FULL);}//always full
    public IDTO get(AuditLog a, DTOLevel lvl) {
        Benutzer bes = a.getBenutzer();

        return new AuditLog.DTO(
                a.getId(), bes!=null?get(bes):null, a.getIpAddress(), a.getUsernameSnapshot(), a.getRoleSnapshot(),
                a.getAction(), a.getPayload(), a.isSuccess(), a.getEndpoint(),
                a.getTimestamp(),
                a.getResourceType(), a.getResourceId()
        );
    }

    public IDTO get(Ticket t){return get(t,DTOLevel.FULL);}
    public IDTO get(Ticket t, DTOLevel lvl) {
        Benutzer cb = t.getCreatedBy();
        Benutzer at = t.getAssignedTo();

        if(lvl == DTOLevel.CARD){
            //todo
        }

        return new Ticket.DTO(
                t.getExternalId(),
                t.getCreationDate(), t.getModificationDate(),
                t.getMessage(), cb==null?null:get(cb),
                t.isClosed(), t.isEscalate(),
                t.getComment(), at==null?null:get(at),
                t.getTicketType(),
                t.getDetails()
        );
    }

    private void assertAdmin(){//todo central
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN")); // standard Spring role format

        if (!isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.MOD_VIEW_NOT_ALLOWED.toString()
            );
        }
    }
}
