package org.example.space_invaders_online.game.database;
import jakarta.persistence.*;

@Entity
@Table(name = "player_stats")
public class PlayerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String playerName;

    @Column(nullable = false)
    private int wins = 0;

    @Column(nullable = false)
    private int totalShots = 0;

    @Column(nullable = false)
    private int totalHits = 0;

    public PlayerStats() {}

    public PlayerStats(String playerName) {
        this.playerName = playerName;
        this.wins = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public void addWin() { this.wins++; }

    public void addShot() { this.totalShots++; }
    public void addHit()  { this.totalHits++;  }
    public int getTotalShots() { return totalShots; }
    public int getTotalHits()  { return totalHits;  }
}
