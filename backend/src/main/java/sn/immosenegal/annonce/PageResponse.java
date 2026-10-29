package sn.immosenegal.annonce;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <T> PageResponse<T> de(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
