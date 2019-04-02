/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.business;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Declares Create, Retrieve, Update and Delete (CRUD) operations for business services.
 *
 * @param <T> Type of the concept managed by this service
 */
public interface CrudService<T> {

    /**
     * Gets all items of the type managed by this service from the repository.
     *
     * @return List of items
     */
    List<T> findAll();

    /**
     * Gets an item with the specified identifier.
     *
     * @param id Item identifier
     * @return Matching item wrapped in an {@code Optional}
     */
    Optional<T> find(URI id);

    /**
     * Gets an item with the specified identifier.
     *
     * @param id Item identifier
     * @return Matching item
     * @throws cz.cvut.kbss.termit.exception.NotFoundException When no matching item is found
     */
    T findRequired(URI id);

    /**
     * Gets a reference to an item with the specified identifier (with empty attribute values).
     *
     * @param id Item identifier
     * @return Matching item reference wrapped in an {@code Optional}
     */
    Optional<T> getReference(URI id);

    /**
     * Gets a reference to an item with the specified identifier (with empty attribute values).
     *
     * @param id Item identifier
     * @return Matching item reference
     * @throws cz.cvut.kbss.termit.exception.NotFoundException When no matching item is found
     */
    T getRequiredReference(URI id);

    /**
     * Checks if an item with the specified identifier exists.
     *
     * @param id Item identifier
     * @return Existence check result
     */
    boolean exists(URI id);

    /**
     * Persists the specified item.
     *
     * @param instance Item to save
     */
    void persist(T instance);

    /**
     * Updates the specified item.
     *
     * @param instance Item update data
     * @return The updated item
     */
    T update(T instance);
}
