package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.util.regex.Pattern;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;
import jenkins.scm.api.mixin.TagSCMHead;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Selection;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class SCMFilter extends SCMSourceTrait {

    @NonNull
    private final String includes;

    @NonNull
    private final String excludes;

    @NonNull
    private final String tagIncludes;

    @NonNull
    private final String tagExcludes;

    @NonNull
    private final String prDestination;

    public String getIncludes() {
        return includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getTagIncludes() {
        return tagIncludes;
    }

    public String getTagExcludes() {
        return tagExcludes;
    }

    public String getPrDestination() {
        return prDestination;
    }

    @DataBoundConstructor
    public SCMFilter(@CheckForNull String includes, String excludes, String tagIncludes, String tagExcludes, String prDestination) {
        this.includes = StringUtils.defaultIfBlank(includes, "*");
        this.excludes = StringUtils.defaultIfBlank(excludes, "");
        this.tagIncludes = StringUtils.defaultIfBlank(tagIncludes, "");
        this.tagExcludes = StringUtils.defaultIfBlank(tagExcludes, "");
        this.prDestination = StringUtils.defaultIfBlank(prDestination, "");
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withPrefilter(new SCMHeadPrefilter() {
            @Override
            public boolean isExcluded(@NonNull SCMSource request, @NonNull SCMHead head) {
                if (head instanceof ChangeRequestSCMHead) {
                    head = ((ChangeRequestSCMHead)head).getTarget();
                    return !Pattern.matches(getPattern(getPrDestination()), head.getName());
                } else if (head instanceof TagSCMHead) {
                    return !Pattern.matches(getPattern(getTagIncludes()), head.getName())
                         || Pattern.matches(getPattern(getTagExcludes()), head.getName());
                } else {
                    return !Pattern.matches(getPattern(getIncludes()), head.getName())
                         || Pattern.matches(getPattern(getExcludes()), head.getName());
                }
            }
        });
    }

    private String getPattern(String branches) {
        StringBuilder quotedBranches = new StringBuilder();
        for (String wildcard : branches.split(" ")) {
            StringBuilder quotedBranch = new StringBuilder();
            for (String branch : wildcard.split("(?=[*])|(?<=[*])")) {
                if (branch.equals("*")) {
                    quotedBranch.append(".*");
                } else if (!branch.isEmpty()) {
                    quotedBranch.append(Pattern.quote(branch));
                }
            }
            if (quotedBranches.length() > 0) {
                quotedBranches.append("|");
            }
            quotedBranches.append(quotedBranch);
        }
        return quotedBranches.toString();
    }

    @Symbol("headSCMFilter")
    @Extension
    @Selection
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.SCMFilter_DisplayName();
        }
    }
}
