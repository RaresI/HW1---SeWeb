<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="userSkillLevel"/>

    <xsl:template match="/recipes">
        <xsl:choose>
            <xsl:when test="count(recipe) = 0">
                <p class="empty">No recipes loaded.</p>
            </xsl:when>
            <xsl:otherwise>
                <table class="xsl-table">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Title</th>
                        <th>Cuisine</th>
                        <th>Difficulty</th>
                    </tr>
                    </thead>
                    <tbody>
                    <xsl:for-each select="recipe">
                        <tr>
                            <xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="difficulty = $userSkillLevel">match-skill</xsl:when>
                                    <xsl:otherwise>other-skill</xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <td><xsl:value-of select="@id"/></td>
                            <td><xsl:value-of select="title"/></td>
                            <td><span class="badge cuisine"><xsl:value-of select="cuisine"/></span></td>
                            <td><span class="badge difficulty"><xsl:value-of select="difficulty"/></span></td>
                        </tr>
                    </xsl:for-each>
                    </tbody>
                </table>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
