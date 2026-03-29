# Frontend TODO — GitHub Integration

Backend will expose the OAuth flow and repo linking endpoints. Here's what the frontend needs to wire up.

## 1. "Connect GitHub" button in room UI

Show a button in the room header/sidebar when no repo is linked yet.

```
[ Connect GitHub Repo ]
```

On click → redirect to `GET /api/auth/github?roomId={roomId}`
Backend handles the OAuth redirect to GitHub from there.

## 2. OAuth callback handling

After GitHub redirects back, the backend will redirect the browser to:
```
{VITE_API_ORIGIN}/rooms/{roomId}?repoLinked=true
```

Frontend needs to:
- Detect the `?repoLinked=true` query param on mount
- Fetch the updated room detail to show the linked repo
- Clear the query param from the URL

## 3. Repo picker (post-auth)

After auth, backend returns a list of repos the user has access to.
Show a simple modal/dropdown to pick one:
```
GET /api/auth/github/repos   → list of { fullName, private, defaultBranch }
POST /api/rooms/{roomId}/repo { repoFullName, branch }  → links repo to room
```

## 4. Linked repo indicator

Once a repo is linked, show it in the room UI:
```
📁 org/repo-name (main)   [ Unlink ]
```

Pull this from the room detail response — backend will add `linkedRepo` to `RoomDetailResponse`.

## 5. Config

No new env vars needed — OAuth flow goes through the backend.
Just make sure `VITE_API_ORIGIN` is set correctly.

## Notes

- Room works fine with no repo linked — GitHub is fully optional
- Token is stored server-side per room, frontend never sees it
- If OAuth fails, backend redirects to `rooms/{roomId}?error=github_auth_failed` — handle that too
