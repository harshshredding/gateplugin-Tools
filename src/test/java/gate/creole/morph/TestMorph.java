package gate.creole.morph;

import gate.Annotation;
import gate.Document;
import gate.Factory;
import gate.creole.ANNIEConstants;
import gate.creole.ExecutionException;
import gate.creole.tokeniser.DefaultTokeniser;
import gate.test.GATEPluginTestCase;
import gate.util.OffsetComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.Ignore;

/**
 * <p>
 * Title: TestMorph
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2000
 * </p>
 * <p>
 * Company: University Of Sheffield
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

@Ignore
public class TestMorph extends GATEPluginTestCase {

	private Morph morpher;

	private Document verbDocumentToTest, verbDocumentWithAnswers,
			nounDocumentToTest, nounDocumentWithAnswers;

	public static int count = 0;

	private DefaultTokeniser tokeniser;

	/**
	 * This method sets up the parameters for the files to be testes It
	 * initialises the Tokenizer and sets up the other parameters for the morph
	 * program
	 */
	@Override
  public void setUp() throws Exception {
		super.setUp();
		try {

			// creating documents
			verbDocumentToTest = Factory.newDocument(this.getClass().getResource("/tests/morph/verbTest.dat"));
			verbDocumentWithAnswers = Factory.newDocument(this.getClass().getResource("/tests/morph/verbAnswer.dat"));
			nounDocumentToTest = Factory.newDocument(this.getClass().getResource("/tests/morph/nounTest.dat"));
			nounDocumentWithAnswers = Factory.newDocument(this.getClass().getResource("/tests/morph/nounAnswer.dat"));
			morpher = (Morph) Factory.createResource("gate.creole.morph.Morph");
			morpher.setAffixFeatureName("affix");
			morpher.setRootFeatureName("root");
			tokeniser = (DefaultTokeniser) Factory
					.createResource("gate.creole.tokeniser.DefaultTokeniser");
		} catch (Exception rie) {
			fail("Resources cannot be created");
		}
	}

	/**
	 * Test the morpher on verbs, if their roots are identified correctly or not
	 */
	public void testAll() {

		// run the tokenizer on the verbTestDocument
		tokeniser.setDocument(verbDocumentToTest);
		tokeniser.setAnnotationSetName("TokeniserAS");
		try {
			tokeniser.execute();
		} catch (ExecutionException ee) {
			fail("Error while executing Tokenizer on the test document");
		}

		// run the tokenizer on the verbAnswerDocument
		tokeniser.setDocument(verbDocumentWithAnswers);
		tokeniser.setAnnotationSetName("TokeniserAS");
		try {
			tokeniser.execute();
		} catch (ExecutionException ee) {
			fail("Error while executing Tokenizer on the test document");
		}

		// now check if the tokenizer was run properly on the document
		List<Annotation> queryTokens = new ArrayList<Annotation>(verbDocumentToTest.getAnnotations(
				"TokeniserAS").get("Token"));
		Collections.sort(queryTokens, new OffsetComparator());

		// same procedure with the answer document
		List<Annotation> answerTokens = new ArrayList<Annotation>(verbDocumentWithAnswers
				.getAnnotations("TokeniserAS").get("Token"));
		Collections.sort(answerTokens, new OffsetComparator());

		// create iterator to get access to each and every individual token
		Iterator<Annotation> queryTokensIter = queryTokens.iterator();
		Iterator<Annotation> answerTokensIter = answerTokens.iterator();

		while (queryTokensIter.hasNext() && answerTokensIter.hasNext()) {

			// get the word to test
			Annotation currentQueryToken = queryTokensIter.next();
			String queryTokenValue = (String) (currentQueryToken.getFeatures()
					.get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME));

			// get the answer of this word
			Annotation currentAnswerToken = answerTokensIter.next();
			String answerTokenValue = (String) (currentAnswerToken
					.getFeatures()
					.get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME));
			// run the morpher
			String rootWord = morpher.findBaseWord(queryTokenValue, "VB");
			// compare it with the answerTokenValue
			assertEquals(rootWord, answerTokenValue);
		}

		// run the tokenizer on the nounTestDocument
		tokeniser.setDocument(nounDocumentToTest);
		tokeniser.setAnnotationSetName("TokeniserAS");
		try {
			tokeniser.execute();
		} catch (ExecutionException ee) {
			fail("Error while executing Tokenizer on the test document");
		}

		// run the tokenizer on the nounAnswerDocument
		tokeniser.setDocument(nounDocumentWithAnswers);
		tokeniser.setAnnotationSetName("TokeniserAS");
		try {
			tokeniser.execute();
		} catch (ExecutionException ee) {
			fail("Error while executing Tokenizer on the test document");
		}

		// check both documents are processed correctly by tokeniser
		assertTrue(!nounDocumentToTest.getAnnotations("TokeniserAS").isEmpty());
		assertTrue(!nounDocumentWithAnswers.getAnnotations("TokeniserAS")
				.isEmpty());


		// now check if the tokenizer was run properly on the document
		queryTokens = new ArrayList<Annotation>(nounDocumentToTest.getAnnotations(
				"TokeniserAS").get("Token"));
		OffsetComparator offsetComparator = new OffsetComparator();
		Collections.sort(queryTokens, offsetComparator);

		// same procedure with the answer document
		answerTokens = new ArrayList<Annotation>(nounDocumentWithAnswers
				.getAnnotations("TokeniserAS").get("Token"));
		Collections.sort(answerTokens, offsetComparator);

		// create iterator to get access to each and every individual token
		queryTokensIter = queryTokens.iterator();
		answerTokensIter = answerTokens.iterator();

		while (queryTokensIter.hasNext() && answerTokensIter.hasNext()) {

			// get the word to test
			Annotation currentQueryToken = queryTokensIter.next();
			String queryTokenValue = (String) (currentQueryToken.getFeatures()
					.get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME));

			// get the answer of this word
			Annotation currentAnswerToken = answerTokensIter
					.next();
			String answerTokenValue = (String) (currentAnswerToken
					.getFeatures()
					.get(ANNIEConstants.TOKEN_STRING_FEATURE_NAME));
			// run the morpher
			String rootWord = morpher.findBaseWord(queryTokenValue, "NN");

			// compare it with the answerTokenValue
			assertEquals(rootWord, answerTokenValue);
		}
	}

	public static Test suite() {
		return new TestSuite(TestMorph.class);
	}
}