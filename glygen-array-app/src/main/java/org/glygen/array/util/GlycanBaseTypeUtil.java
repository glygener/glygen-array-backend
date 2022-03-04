package org.glygen.array.util;

import java.util.List;

import org.eurocarbdb.MolecularFramework.sugar.Anomer;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Modification;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;

public class GlycanBaseTypeUtil
{

    public static Monosaccharide getReducingEnd(Sugar a_sugar) throws GlycoconjugateException
    {
        List<GlycoNode> t_roots = a_sugar.getRootNodes();
        if (t_roots.size() != 1)
        {
            throw new GlycoconjugateException("There is no single root node in the sugar.");
        }
        GlycoNode t_rootNode = t_roots.iterator().next();
        if (!t_rootNode.getClass().equals(Monosaccharide.class))
        {
            throw new GlycoconjugateException("Root node is not a monosaccharide.");
        }
        return (Monosaccharide) t_rootNode;
    }

    /**
     * Create the basetype version (unknown anomer) of the sugar
     *
     * @param a_sugar
     *            Sugar to modify
     * @throws GlycoconjugateException
     *             if its not possible to create a single reducing end
     *             monosaccharide or the creation of the unknown anomer fails
     */
    public static void makeBaseType(Sugar a_sugar) throws GlycoconjugateException
    {
        // get reducing end monosaccharide
        Monosaccharide t_ms = GlycanBaseTypeUtil.getReducingEnd(a_sugar);
        t_ms.setAnomer(Anomer.Unknown);
    }

    /**
     * Create the alpha version of the sugar
     *
     * @param a_sugar
     *            Sugar to modify
     * @throws GlycoconjugateException
     *             if its not possible to create a single reducing end
     *             monosaccharide or the creation of the alpha anomer fails
     */
    public static void makeAlpha(Sugar a_sugar) throws GlycoconjugateException
    {
        Monosaccharide t_ms = GlycanBaseTypeUtil.getReducingEnd(a_sugar);
        t_ms.setAnomer(Anomer.Alpha);
    }

    /**
     * Create the beta version of the sugar
     *
     * @param a_sugar
     *            Sugar to modify
     * @throws GlycoconjugateException
     *             if its not possible to create a single reducing end
     *             monosaccharide or the creation of the beta anomer fails
     */
    public static void makeBeta(Sugar a_sugar) throws GlycoconjugateException
    {
        Monosaccharide t_ms = GlycanBaseTypeUtil.getReducingEnd(a_sugar);
        t_ms.setAnomer(Anomer.Beta);
    }

    /**
     * Create the alditol version of the sugar
     *
     * @param a_sugar
     *            Sugar to modify
     * @throws GlycoconjugateException
     *             if its not possible to create a single reducing end
     *             monosaccharide or the creation of the alditol fails
     */
    public static void makeAlditol(Sugar a_sugar) throws GlycoconjugateException
    {
        Monosaccharide t_ms = GlycanBaseTypeUtil.getReducingEnd(a_sugar);
        t_ms.setAnomer(Anomer.OpenChain);
        List<Modification> t_modification = t_ms.getModification();
        Modification t_mod = new Modification(ModificationType.ALDI, 1);
        t_modification.add(t_mod);
        t_ms.setRing(Monosaccharide.OPEN_CHAIN, Monosaccharide.OPEN_CHAIN);
    }

    /**
     * Method to check if we can make a basetype and all other 3 versions.
     *
     * @param a_sugar
     *            Sugar to test
     * @return true, if creation of all 4 versions is possible. false, if the
     *         reducing end monosaccharide ring is not set or is an alditol
     * @throws GlycoconjugateException
     *             thrown if its not possible to get the reducing end
     *             monosaccharide or if there are more than one monosaccharides
     *             without parent (composition)
     */
    public static boolean isMakeBaseTypePossible(Sugar a_sugar) throws GlycoconjugateException
    {
        Monosaccharide t_ms = GlycanBaseTypeUtil.getReducingEnd(a_sugar);
        if (t_ms.getRingStart() < 1)
        {
            return false;
        }
        List<Modification> t_modifications = t_ms.getModification();
        for (Modification t_mod : t_modifications)
        {
            if (t_mod.getModificationType().equals(ModificationType.ALDI))
            {
                return false;
            }
        }
        return true;
    }
}
