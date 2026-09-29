import { useState, type FormEvent, type KeyboardEvent } from "react";
import type { DeskDetail } from "@atlas/data";

type Props = {
  /** The desk being edited; leave out to create a new one (then the key is typed too). */
  desk?: DeskDetail;
  onSave: (desk: DeskDetail) => void;
  onCancel: () => void;
  /** The API's reason when the last save was refused. */
  error?: string;
};

const fieldStyle = { display: "flex", flexDirection: "column", gap: 4, fontSize: 14 } as const;
const inputStyle = { padding: "8px 10px", fontSize: 15, borderRadius: 4, border: "1px solid #3a424c", background: "#0b0d10", color: "#e8ebef" } as const;
const buttonStyle = { padding: "8px 14px", borderRadius: 4, fontSize: 15, border: "1px solid #3a424c", background: "transparent", color: "#e6e6e6", cursor: "pointer" } as const;

/** The New / Edit desk form: name, description and members as email chips. What it shows is what onSave gets. */
export function DeskEditor({ desk, onSave, onCancel, error }: Props) {
  const [key, setKey] = useState(desk?.key ?? "");
  const [name, setName] = useState(desk?.name ?? "");
  const [description, setDescription] = useState(desk?.description ?? "");
  const [members, setMembers] = useState<string[]>(desk?.members ?? []);
  const [draft, setDraft] = useState("");

  const addMember = () => {
    const email = draft.trim().toLowerCase();
    if (email !== "" && !members.includes(email)) {
      setMembers([...members, email]);
    }
    setDraft("");
  };

  const onDraftKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter" || event.key === ",") {
      event.preventDefault();
      addMember();
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSave({ key, name, description, members });
  };

  return (
    <dialog open style={{ background: "#171b21", color: "#e8ebef", border: "1px solid #2a3038", borderRadius: 8, padding: 24, width: 520 }}>
      <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        <h2 style={{ margin: 0, fontSize: 20 }}>{desk ? `Edit ${desk.name}` : "New desk"}</h2>

        {desk
          ? <div style={fieldStyle}><span style={{ color: "#9aa3ad" }}>Key</span><span>{desk.key}</span></div>
          : <label style={fieldStyle}>Key<input value={key} onChange={e => setKey(e.target.value)} placeholder="e.g. lng" style={inputStyle} /></label>}
        <label style={fieldStyle}>Name<input value={name} onChange={e => setName(e.target.value)} style={inputStyle} /></label>
        <label style={fieldStyle}>Description<input value={description} onChange={e => setDescription(e.target.value)} style={inputStyle} /></label>

        <div style={fieldStyle}>
          <span id="desk-members">Members</span>
          <ul aria-labelledby="desk-members" style={{ display: "flex", flexWrap: "wrap", gap: 8, listStyle: "none", margin: 0, padding: 0 }}>
            {members.map(email => (
              <li key={email} style={{ display: "inline-flex", alignItems: "center", gap: 6, padding: "4px 10px", borderRadius: 14, border: "1px solid #3a424c" }}>
                {email}
                <button type="button" aria-label={`Remove ${email}`} onClick={() => setMembers(members.filter(m => m !== email))}
                  style={{ ...buttonStyle, padding: "0 4px", border: "none", color: "#9aa3ad" }}>×</button>
              </li>
            ))}
          </ul>
          <label style={fieldStyle}>Add a member by email
            <input value={draft} onChange={e => setDraft(e.target.value)} onKeyDown={onDraftKeyDown} onBlur={addMember}
              placeholder="name@braemar.com, then Enter" style={inputStyle} />
          </label>
        </div>

        {error && <p role="alert" style={{ margin: 0, color: "#f2b632" }}>{error}</p>}

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 10 }}>
          <button type="button" onClick={onCancel} style={buttonStyle}>Cancel</button>
          <button type="submit" style={{ ...buttonStyle, background: "#2fcf75", color: "#06210f", fontWeight: 700 }}>Save</button>
        </div>
      </form>
    </dialog>
  );
}
