package com.artemhontar.fluxdigitalstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "reset_password_token")
public class ResetPasswordToken implements Comparable<ResetPasswordToken> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;


    @Column(name = "expiry_date_in_milliseconds", nullable = false)
    private Timestamp expiryDateInMilliseconds;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "local_user_id", nullable = false)
    private LocalUser localUser;

    @Column(name = "token", nullable = false, unique = true, length = 1024)
    private String token;

    @Column(name = "is_token_used", nullable = false)
    @ColumnDefault("false")
    private Boolean isTokenUsed = false;

    /**
     * Compares this token to the specified object based on its expiry date.
     * Tokens that expire sooner (older Timestamp) are considered "less than"
     * those that expire later (newer Timestamp).
     * * @param other the token to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this token
     * is less than, equal to, or greater than the specified token.
     */
    @Override
    public int compareTo(ResetPasswordToken other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare with a null object.");
        }

        return Objects.compare(
                this.expiryDateInMilliseconds,
                other.expiryDateInMilliseconds,
                Comparator.nullsFirst(Timestamp::compareTo)
        );

    }

    /**
     * Determines if two ResetPasswordToken objects are equal.
     * For JPA entities, it's standard practice to base equality on the
     * business key (often 'token') or the primary key ('id').
     * Since 'token' is unique and defined immediately upon creation,
     * it's a good candidate for business equality before the ID is set.
     * We will use the 'token' field for business equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Check for exact class type
        if (o == null || getClass() != o.getClass()) return false;

        ResetPasswordToken that = (ResetPasswordToken) o;

        // Use 'token' as the business key for equality. It is unique and non-null.
        return Objects.equals(token, that.token);
    }

    /**
     * Generates a hash code consistent with the equals method.
     */
    @Override
    public int hashCode() {
        // Generate hash code based on the 'token' field.
        return Objects.hash(token);
    }
}