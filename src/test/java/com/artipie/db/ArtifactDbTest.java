/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.db;

import com.amihaiemil.eoyaml.Yaml;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for artifacts db.
 * @since 0.31
 * @checkstyle MagicNumberCheck (1000 lines)
 */
class ArtifactDbTest {

    @Test
    void createsTable(final @TempDir Path path) throws SQLException {
        final DataSource source = new ArtifactDbFactory(
            Yaml.createYamlMappingBuilder().add(
                "artifacts_database",
                Yaml.createYamlMappingBuilder().add(
                    ArtifactDbFactory.PATH,
                    path.resolve("test.db").toString()
                ).build()
            ).build()
        ).initialize();
        try (
            Connection conn = source.getConnection();
            Statement stat = conn.createStatement()
        ) {
            stat.execute("select count(*) from artifacts");
            MatcherAssert.assertThat(
                stat.getResultSet().getInt(1),
                new IsEqual<>(0)
            );
        }
    }

}
