package com.itibari.auth.repository.customized.impl;

import com.itibari.auth.model.Country;
import com.itibari.auth.model.response.CountryResponse;
import com.itibari.auth.repository.customized.CountryRepositoryCustomized;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class CountryRepositoryCustomizedImpl implements CountryRepositoryCustomized {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<CountryResponse> list(String searchParam, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CountryResponse> query = criteriaBuilder.createQuery(CountryResponse.class);
        Root<Country> root = query.from(Country.class);
        query.select(criteriaBuilder.construct(CountryResponse.class,
                        root.get("publicId"),
                        root.get("name"),
                        root.get("isoAlphaThree"),
                        root.get("phoneCode")))
                .where(filters(criteriaBuilder, root, searchParam))
                .orderBy(criteriaBuilder.asc(root.get("name")));

        TypedQuery<CountryResponse> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Country> countRoot = countQuery.from(Country.class);
        countQuery.select(criteriaBuilder.count(countRoot))
                .where(filters(criteriaBuilder, countRoot, searchParam));

        return new PageImpl<>(typedQuery.getResultList(), pageable,
                entityManager.createQuery(countQuery).getSingleResult());
    }

    private Predicate[] filters(CriteriaBuilder criteriaBuilder, Root<Country> root, String searchParam) {
        List<Predicate> filters = new ArrayList<>();
        filters.add(criteriaBuilder.isFalse(root.get("deleted")));
        if (searchParam != null && !searchParam.isBlank()) {
            String search = "%" + searchParam.trim().toLowerCase() + "%";
            filters.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), search),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("isoAlphaThree")), search),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneCode")), search)));
        }
        return filters.toArray(Predicate[]::new);
    }
}
