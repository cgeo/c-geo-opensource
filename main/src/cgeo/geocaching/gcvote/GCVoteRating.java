package cgeo.geocaching.gcvote;

public final class GCVoteRating {
    private final float rating;
    private final int votes;
    private final float myVote;

    public GCVoteRating(float rating, int votes, float myVote) {
        this.rating = rating;
        this.votes = votes;
        this.myVote = myVote;
    }

    public float getRating() {
        return rating;
    }

    public int getVotes() {
        return votes;
    }

    public float getMyVote() {
        return myVote;
    }
}
