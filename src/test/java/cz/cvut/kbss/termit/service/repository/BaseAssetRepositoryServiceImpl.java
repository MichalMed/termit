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
package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Validator;

public class BaseAssetRepositoryServiceImpl extends BaseAssetRepositoryService<Vocabulary> {

    private final VocabularyDao dao;

    @Autowired
    public BaseAssetRepositoryServiceImpl(VocabularyDao dao, Validator validator) {
        super(validator);
        this.dao = dao;
    }

    @Override
    protected AssetDao<Vocabulary> getPrimaryDao() {
        return dao;
    }
}
