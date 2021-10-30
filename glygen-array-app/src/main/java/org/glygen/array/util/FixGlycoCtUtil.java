package org.glygen.array.util;

import java.util.List;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.Linkage;
import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.sugar.UnderdeterminedSubTree;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorNodeType;

public class FixGlycoCtUtil
{
    private Sugar m_sugar = null;

    /**
     * Takes a GlycoCT sequence and fixes common problems.
     *
     * @param a_glycoCT
     *            String with the GlycoCT sequence
     * @return Recoded GlycoCT sequence with problems fixed.
     * @throws GlycoVisitorException
     *             If the fixing fails
     */
    public String fixGlycoCT(String a_glycoCT) throws GlycoVisitorException
    {
        try
        {
            SugarImporterGlycoCTCondensed t_importer = new SugarImporterGlycoCTCondensed();
            this.m_sugar = t_importer.parse(a_glycoCT);

            this.fixUndLinkage();

            SugarExporterGlycoCTCondensed t_exporter = new SugarExporterGlycoCTCondensed();
            t_exporter.start(this.m_sugar);
            return t_exporter.getHashCode();
        }
        catch (SugarImporterException e)
        {
            throw new GlycoVisitorException("Unable to parse GlycoCT", e);
        }

    }

    private void fixUndLinkage() throws GlycoVisitorException
    {
        try
        {

            GlycoVisitorNodeType t_visitorType = new GlycoVisitorNodeType();
            List<UnderdeterminedSubTree> t_subtrees = this.m_sugar.getUndeterminedSubTrees();
            for (UnderdeterminedSubTree t_underdeterminedSubTree : t_subtrees)
            {
                GlycoEdge t_edge = t_underdeterminedSubTree.getConnection();
                List<GlycoNode> t_nodes = t_underdeterminedSubTree.getParents();
                for (GlycoNode t_glycoNode : t_nodes)
                {
                    if (!t_visitorType.isMonosaccharide(t_glycoNode))
                    {
                        throw new GlycoVisitorException(
                                "UND partent nodes contains non-monosaccharides.");
                    }
                }
                t_nodes = t_underdeterminedSubTree.getRootNodes();
                for (GlycoNode t_glycoNode : t_nodes)
                {
                    if (!t_visitorType.isMonosaccharide(t_glycoNode))
                    {
                        throw new GlycoVisitorException(
                                "UND root nodes contains non-monosaccharides.");
                    }
                }
                // both parents and root nodes are monosaccharides. So its a
                // glycosidic linkage.
                for (Linkage t_linkage : t_edge.getGlycosidicLinkages())
                {
                    t_linkage.setParentLinkageType(LinkageType.H_AT_OH);
                    t_linkage.setChildLinkageType(LinkageType.DEOXY);
                }
            }
        }
        catch (Exception e)
        {
            throw new GlycoVisitorException("Error trying to fix UND linkage: " + e.getMessage(),
                    e);
        }
    }

}
