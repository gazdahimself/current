/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.model;

import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.james.mailbox.name.AbstractMailboxName;
import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;
import org.apache.james.mailbox.name.MailboxName;


/**
 * Expresses select criteria for mailboxes.
 */
public final class MailboxQuery {

    
    public static class DefaultMailboxNameSerializer implements MailboxNameSerializer {
        /**
         * Form feed handled as white space by {@link Pattern}. 
         * Very unprobable in names - hence we will not 
         * undertake any attempts to escape it.
         */
        protected static final char DELIMETER = '\f';
        protected static final String REGEX_LOCAL = "[^"+ DELIMETER +"]*";
        protected void escape(String segment, StringBuilder result) {
            result.append(segment);
        }
        public String serialize(AbstractMailboxName mailboxName) {
            StringBuilder result = new StringBuilder(DefaultUnresolvedMailboxName.estimateSerializedLength(mailboxName));
            ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
            while (segmentsIt.hasNext()) {
                if (result.length() > 0) {
                    result.append(DELIMETER);
                }
                escape(segmentsIt.next(), result);
            }
            return result.toString();
        }

    }
    /**
     * A non-wild matcher.
     */
    public static class DomesticMailboxNameMatcher implements MailboxNameMatcher {
        protected MailboxName patternName;

        public DomesticMailboxNameMatcher(MailboxName patternName) {
            super();
            this.patternName = patternName;
        }

        /**
         * @see org.apache.james.mailbox.model.MailboxQuery.MailboxNameMatcher#matches(org.apache.james.mailbox.name.MailboxName)
         */
        @Override
        public boolean matches(MailboxName name) {
            return patternName.equals(name);
        }
    }
    public static class LocalOnlyMailboxNameMatcher extends DomesticMailboxNameMatcher implements MailboxNameMatcher {
        
        private Pattern[] segmentPatterns;

        public LocalOnlyMailboxNameMatcher(MailboxName patternName, int firstLocalWildcardAt) {
            super(patternName);
            int length = patternName.getSegmentCount();
            segmentPatterns = new Pattern[length];
            int i = firstLocalWildcardAt;
            segmentPatterns[i] = Pattern.compile(regex(patternName.getSegmentAt(i)));
            i++;
            while (i < length) {
                String pattern = regex(patternName.getSegmentAt(i));
                if (pattern != null) {
                    segmentPatterns[i] = Pattern.compile(pattern);
                }
                i++;
            }
        }
        
        @Override
        public boolean matches(MailboxName subject) {
            int subjectLength = subject.getSegmentCount();
            if (subjectLength != patternName.getSegmentCount()) {
                return false;
            }
            else {
                for (int i = 0; i < segmentPatterns.length; i++) {
                    String subjectSegment = subject.getSegmentAt(i);
                    Pattern pat = segmentPatterns[i];
                    if (pat != null) {
                        if (!pat.matcher(subjectSegment).matches()) {
                            return false;
                        }
                    }
                    else {
                        String patternSegment = patternName.getSegmentAt(i);
                        if (!patternSegment.equals(subjectSegment)) {
                            return false;
                        }
                    }
                }                
            }
            return true;
        }

        private String regex(String segment) {
            StringTokenizer st = new StringTokenizer(segment, LOCALWILDCARD_STRING, true);
            StringBuilder sb = new StringBuilder(segment.length() + 8);
            while (st.hasMoreElements()) {
                String token = st.nextToken();
                if (token.length() == 1 && token.charAt(0) == LOCALWILDCARD) {
                    sb.append(REGEX_FREE);
                }
                else {
                    sb.append(Pattern.quote(token));
                }
            }
            return sb.toString();
        }
    }
    public interface MailboxNameMatcher {
        boolean matches(MailboxName name);
    }
    public interface MailboxNameSerializer {
        public String serialize(AbstractMailboxName mailboxName);
    }
    private static class PatternMailboxNameSerializer extends DefaultMailboxNameSerializer {

        @Override
        protected void escape(String segment, StringBuilder sb) {
            StringTokenizer st = new StringTokenizer(segment, LOCALWILDCARD_FREEWILDCARD_STRING, true);
            while (st.hasMoreElements()) {
                String token = st.nextToken();
                if (token.length() == 1) {
                    switch (token.charAt(0)) {
                    case LOCALWILDCARD:
                        sb.append(REGEX_LOCAL);
                        break;
                    case FREEWILDCARD:
                        sb.append(REGEX_FREE);
                        break;
                    default:
                        sb.append(Pattern.quote(token));
                        break;
                    }
                }
                else {
                    sb.append(Pattern.quote(token));
                }
            }
        }
        
    }
    public static class WildMailboxNameMatcher implements MailboxNameMatcher {
        
        private final Pattern pattern;

        public WildMailboxNameMatcher(MailboxName patternName) {
            this.pattern = Pattern.compile(PATTERN_SERIALIZER.serialize(patternName));
        }

        @Override
        public boolean matches(MailboxName name) {
            String serializedName = NAME_SERIALIZER.serialize(name);
            return pattern.matcher(serializedName).matches();
        }
        
    }
    
    /**
     * Use this wildcard to match every char including the hierarchy delimiter
     */
    public final static char FREEWILDCARD = '*';
    
    /**
     * Use this wildcard to match every char except the hierarchy delimiter
     */
    public static final char LOCALWILDCARD = '%';

    public static final String LOCALWILDCARD_FREEWILDCARD_STRING;

    public static final String LOCALWILDCARD_STRING;
    public static final String FREEWILDCARD_STRING;
    
    private static final DefaultMailboxNameSerializer NAME_SERIALIZER = new DefaultMailboxNameSerializer();
    private static final PatternMailboxNameSerializer PATTERN_SERIALIZER = new PatternMailboxNameSerializer();

    private static final String REGEX_FREE = ".*";

    static {
        LOCALWILDCARD_STRING = String.valueOf(LOCALWILDCARD);
        LOCALWILDCARD_FREEWILDCARD_STRING = new String(new char[] {LOCALWILDCARD, FREEWILDCARD});
        FREEWILDCARD_STRING = String.valueOf(FREEWILDCARD);
    }

    private final MailboxName base;
    
    
    private final MailboxName expression;
    
    private final MailboxNameMatcher matcher;
    private final MailboxName resolvedExpression;
    
    /**
     * Constructs an expression determining a set of mailbox names.
     * 
     * @param base
     *            base reference name, not null
     * @param expression
     *            mailbox match expression, not null
     * @param pathDelimiter
     *            path delimiter to use
     */
    public MailboxQuery(final MailboxName base, final MailboxName expression) {
        super();
        if (base == null) {
            throw new IllegalArgumentException("A non-null base needed to create a new "+ MailboxQuery.class.getName());
        }
        if (expression == null) {
            throw new IllegalArgumentException("A non-null expression needed to create a new "+ MailboxQuery.class.getName());
        }
        
        this.base = base;
        this.expression = expression;
        MailboxName resolvedExpression = base.relative(expression);
        boolean startsWithWildCard = false;
        if (resolvedExpression.getSegmentCount() > 0) {
            String firstSegment = resolvedExpression.getSegmentAt(0);
            if (firstSegment.length() > 0) {
                switch (firstSegment.charAt(0)) {
                case LOCALWILDCARD:
                case FREEWILDCARD:
                    startsWithWildCard = true;
                    break;
                }
            }
        }
        if (!resolvedExpression.hasRoot() && !startsWithWildCard) {
            throw new IllegalStateException("Could not combine a rooted mailbox name out of base = '"+ base +"'  and expression = '"+ expression +"'.");
        }
        this.resolvedExpression = resolvedExpression;

        matcher = createMatcher(resolvedExpression);
    }
    
    public MailboxQuery(final MailboxName qualifiedExpression) {
        if (qualifiedExpression == null || !qualifiedExpression.hasRoot()) {
            throw new IllegalArgumentException("A non-null, rooted expression needed to create a new "+ MailboxQuery.class.getName());
        }
        this.base = MailboxName.EMPTY;
        this.expression = qualifiedExpression;
        this.resolvedExpression = qualifiedExpression;

        matcher = createMatcher(resolvedExpression);
    }

    /**
     * TODO createMatcher.
     *
     * @param rootedCanonicalExpression
     * @return
     */
    private MailboxNameMatcher createMatcher(MailboxName rootedCanonicalExpression) {
        ListIterator<String> segmentsIt = rootedCanonicalExpression.segmentsIterator();
        int firstLocalWildCardAt = -1;
        int i = 0;
        while (segmentsIt.hasNext()) {
            String segment = segmentsIt.next();
            for (int j = 0; j < segment.length(); j++) {
                switch (segment.charAt(j)) {
                case LOCALWILDCARD:
                    if (firstLocalWildCardAt < 0) {
                        firstLocalWildCardAt = i;
                    }
                    break;
                case FREEWILDCARD:
                    return new WildMailboxNameMatcher(rootedCanonicalExpression);
                default:
                    break;
                }
            }
            i++;
        }
        if (firstLocalWildCardAt >= 0) {
            return new LocalOnlyMailboxNameMatcher(rootedCanonicalExpression, firstLocalWildCardAt);
        }
        else {
            return new DomesticMailboxNameMatcher(rootedCanonicalExpression);
        }
    }

    /**
     * Gets the base reference for the search.
     * 
     * @return the base
     */
    public final MailboxName getBase() {
        return base;
    }

    /**
     * Gets the name search expression. This may contain wildcards.
     * 
     * @return the expression
     */
    public final MailboxName getExpression() {
        return expression;
    }

    /**
     * Gets wildcard character that matches any series of characters.
     * 
     * @return the freeWildcard
     */
    public final char getFreeWildcard() {
        return FREEWILDCARD;
    }

    /**
     * Gets wildcard character that matches any series of characters excluding
     * hierarchy delimiters. Effectively, this means that it matches any
     * sequence within a name part.
     * 
     * @return the localWildcard
     */
    public final char getLocalWildcard() {
        return LOCALWILDCARD;
    }

    public MailboxName getResolvedExpression() {
        return resolvedExpression;
    }

    /**
     * Is the given name a match for {@link #getExpression()}?
     * 
     * @param name
     *            name to be matched
     * @return true if the given name matches this expression, false otherwise
     */
    public final boolean isExpressionMatch(MailboxName name) {
        return matcher.matches(name);
    }
    
    /**
     * Renders a string suitable for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";
        return "MailboxExpression [ " + "base = " + this.base + TAB + "expression = " + this.expression + TAB + "freeWildcard = " + this.getFreeWildcard() + TAB + "localWildcard = " + this.getLocalWildcard() + TAB + " ]";
    }

    /**
     * TODO containsWidcard.
     *
     * @param domain
     * @return
     */
    public static boolean containsWidcard(String nameSegment) {
        for (int i = 0; i < nameSegment.length(); i++) {
            switch (nameSegment.charAt(i)) {
            case LOCALWILDCARD:
            case FREEWILDCARD:
                return true;
            }
        }
        return false;
    }

}
