package compiler.symboltable;

import java.util.ArrayList;

import compiler.helper.SymbolContext;

public class LazyMethodEvaluator
{
    private Symboltable symTabClass;

    private ArrayList<MethodEvalStore> methodEvalListPut = new ArrayList<>();

    private ArrayList<MethodEvalStore> methodEvalListGet = new ArrayList<>();

    public LazyMethodEvaluator( Symboltable symTabClass )
    {
        this.symTabClass = symTabClass;
    }

    public class MethodEvalStore
    {
        public Objekt methodObj;

        public SymbolContext con;

        public MethodEvalStore( Objekt methodObj, SymbolContext con )
        {
            this.methodObj = methodObj;
            this.con = con;
        }
    }

    /**
     * @outdated
     * Methods are added lazy because the parameters are assigned after the object is put,
     * making an on time evaluation pointless.
     * Currently only supports checking of the methods name was defined!
     * BUT i think I check if the method call or assignment via method is correct,
     * will be done in the AST.
     * TYPE check will be done later BUT para list evaluation no idea.
     */
    public void startMethodObjektEvaluation()
    {
        /* first add all method objekts, so paras had time to be added */
        for ( MethodEvalStore methodEval : methodEvalListPut )
        {
            symTabClass.putObjekt( methodEval.methodObj, methodEval.con );
        }

        /* then evaluate all method calls */
        for ( MethodEvalStore methodEval : methodEvalListGet )
        {
            symTabClass.getObject( methodEval.methodObj, methodEval.con );
        }
    }

    public void lazyMethodObjektPut( Objekt methodObj, SymbolContext lastCon )
    {
        methodEvalListPut.add( new MethodEvalStore( methodObj, lastCon ) );
    }

    public void lazyMethodObjektGet( Objekt signature, SymbolContext lastCon )
    {
        methodEvalListGet.add( new MethodEvalStore( signature, lastCon ) );
    }
}

