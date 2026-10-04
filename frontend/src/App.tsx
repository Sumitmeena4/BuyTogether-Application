import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleHelp,
  ClipboardList,
  LoaderCircle,
  MessageCircle,
  Plus,
  ShoppingBasket,
  Sparkles,
  Trash2,
  WandSparkles,
  X,
} from "lucide-react";
import { buyTogetherApi, errorMessage } from "./api";
import type { GroupSummary, OrderItem, ShoppingGroup, ShoppingRequest } from "./types";

const demoConversation = `Rahul: I need 3 blue pens for the workshop.
Priya: Can someone get me 2 spiral notebooks?
Aman: I need five blue pens too.
Neha: I'll need one spiral notebook.
Rahul: Also get me one black marker.
Aman: Actually make that two black markers for me.`;

type View = "dashboard" | "workspace";

function App() {
  const [groups, setGroups] = useState<GroupSummary[]>([]);
  const [group, setGroup] = useState<ShoppingGroup | null>(null);
  const [view, setView] = useState<View>("dashboard");
  const [name, setName] = useState("");
  const [conversation, setConversation] = useState("");
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [processingStep, setProcessingStep] = useState(0);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [sourceRequest, setSourceRequest] = useState<ShoppingRequest | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<OrderItem | null>(null);

  const refreshGroups = async () => {
    try {
      setGroups(await buyTogetherApi.listGroups());
    } catch (loadError) {
      setError(`Could not load shopping groups: ${errorMessage(loadError)}`);
    }
  };

  useEffect(() => {
    void refreshGroups();
  }, []);

  useEffect(() => {
    if (!analyzing) return;
    const interval = window.setInterval(() => {
      setProcessingStep((step) => Math.min(step + 1, 3));
    }, 1100);
    return () => window.clearInterval(interval);
  }, [analyzing]);

  const openGroup = async (id: string) => {
    setLoading(true);
    setError("");
    try {
      setGroup(await buyTogetherApi.getGroup(id));
      setConversation("");
      setView("workspace");
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  };

  const createGroup = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!name.trim()) return;
    setLoading(true);
    setError("");
    try {
      const created = await buyTogetherApi.createGroup(name.trim());
      setName("");
      await refreshGroups();
      await openGroup(created.id);
    } catch (createError) {
      setError(errorMessage(createError));
    } finally {
      setLoading(false);
    }
  };

  const analyze = async () => {
    if (!group || !conversation.trim()) return;
    setProcessingStep(0);
    setAnalyzing(true);
    setError("");
    setNotice("");
    try {
      const result = await buyTogetherApi.analyze(group.id, conversation);
      setGroup(result.group);
      setNotice(`Gemma found ${result.requestCount} requests and Java created ${result.productCount} products.`);
      await refreshGroups();
    } catch (analysisError) {
      setError(`Analysis failed: ${errorMessage(analysisError)}`);
    } finally {
      setAnalyzing(false);
    }
  };

  const updateGroup = (nextGroup: ShoppingGroup) => setGroup(nextGroup);

  const deleteItem = async (itemId: string) => {
    if (!group) return;
    try {
      await buyTogetherApi.deleteItem(itemId);
      updateGroup({ ...group, items: group.items.filter((item) => item.id !== itemId) });
      await refreshGroups();
    } catch (deleteError) {
      setError(errorMessage(deleteError));
    }
  };

  const changeStatus = async (item: OrderItem) => {
    if (!group) return;
    try {
      const updated = await buyTogetherApi.updateStatus(
        item.id,
        item.status === "COMPLETED" ? "OPEN" : "COMPLETED",
      );
      updateGroup({ ...group, items: group.items.map((current) => current.id === item.id ? updated : current) });
      await refreshGroups();
    } catch (statusError) {
      setError(errorMessage(statusError));
    }
  };

  const editItem = (item: OrderItem) => {
    setEditingItem(item);
  };

  const saveItem = async (item: OrderItem, nextName: string, nextQuantity: number, nextUnit: string) => {
    if (!group) return;
    if (!nextName.trim() || !Number.isInteger(nextQuantity) || nextQuantity <= 0 || !nextUnit.trim()) {
      setError("Enter a product, positive whole-number quantity, and unit.");
      return false;
    }
    try {
      const updated = await buyTogetherApi.updateItem({
        ...item,
        name: nextName.trim(),
        totalQuantity: nextQuantity,
        unit: nextUnit.trim(),
      });
      updateGroup({ ...group, items: group.items.map((current) => current.id === item.id ? updated : current) });
      setEditingItem(null);
      return true;
    } catch (updateError) {
      setError(errorMessage(updateError));
      return false;
    }
  };

  const addItem = async (itemName: string, quantity: number, unit: string) => {
    if (!group) return;
    try {
      const added = await buyTogetherApi.addItem(group.id, itemName, quantity, unit);
      updateGroup({ ...group, items: [...group.items, added].sort((a, b) => a.name.localeCompare(b.name)) });
      setAddOpen(false);
      await refreshGroups();
    } catch (addError) {
      setError(errorMessage(addError));
    }
  };

  const goHome = () => {
    setView("dashboard");
    setGroup(null);
    setError("");
    setNotice("");
    void refreshGroups();
  };

  return (
    <div className="min-h-screen bg-[#f7f8f6] text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-[1200px] items-center justify-between px-5 py-3 sm:px-8">
          <button onClick={goHome} className="flex items-center gap-3 text-left" aria-label="BuyTogether dashboard">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-emerald-700 text-white">
              <ShoppingBasket size={21} strokeWidth={2.4} />
            </span>
            <span className="text-base font-bold tracking-tight">BuyTogether</span>
          </button>
          <div className="flex items-center gap-3">
            {view === "workspace" && (
              <button onClick={goHome} className="button-quiet">
                <ArrowLeft size={16} /> <span className="hidden sm:inline">All groups</span>
              </button>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1200px] px-5 pb-12 pt-6 sm:px-8 sm:pt-8">
        {error && (
          <div role="alert" className="mb-5 flex items-start justify-between gap-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
            <span>{error}</span>
            <button aria-label="Dismiss error" onClick={() => setError("")}><X size={17} /></button>
          </div>
        )}
        {notice && (
          <div role="status" className="mb-5 flex items-center justify-between gap-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-900">
            <span>{notice}</span>
            <button aria-label="Dismiss message" onClick={() => setNotice("")}><X size={17} /></button>
          </div>
        )}
        {view === "dashboard" ? (
          <Dashboard
            groups={groups}
            name={name}
            loading={loading}
            onNameChange={setName}
            onCreate={createGroup}
            onOpen={openGroup}
          />
        ) : group ? (
          <Workspace
            group={group}
            conversation={conversation}
            loading={analyzing}
            processingStep={processingStep}
            sourceRequest={sourceRequest}
            addOpen={addOpen}
            editingItem={editingItem}
            onHome={goHome}
            onConversationChange={setConversation}
            onDemo={() => { setConversation(demoConversation); setNotice("Demo conversation loaded. Analyze it with the live Gemma 4 API."); }}
            onAnalyze={analyze}
            onRequestClick={setSourceRequest}
            onCloseSource={() => setSourceRequest(null)}
            onEdit={editItem}
            onSaveEdit={saveItem}
            onCloseEdit={() => setEditingItem(null)}
            onDelete={deleteItem}
            onStatus={changeStatus}
            onAdd={() => setAddOpen(true)}
            onCloseAdd={() => setAddOpen(false)}
            onAddSubmit={addItem}
          />
        ) : (
          <div className="grid min-h-[50vh] place-items-center text-slate-500"><LoaderCircle className="animate-spin" /></div>
        )}
      </main>
      <footer className="mx-auto flex max-w-[1200px] flex-col gap-2 px-5 pb-6 text-xs text-slate-500 sm:flex-row sm:justify-between sm:px-8">
        <span>BuyTogether · Shared shopping, made clear.</span>
        <span>Group chat → structured requests → shared order</span>
      </footer>
    </div>
  );
}

type DashboardProps = {
  groups: GroupSummary[];
  name: string;
  loading: boolean;
  onNameChange: (name: string) => void;
  onCreate: (event: React.FormEvent<HTMLFormElement>) => void;
  onOpen: (id: string) => void;
};

function Dashboard({ groups, name, loading, onNameChange, onCreate, onOpen }: DashboardProps) {
  const [showCreate, setShowCreate] = useState(false);
  return (
    <div className="animate-in">
      <section aria-labelledby="groups-title">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="eyebrow">Your workspace</p>
            <h1 id="groups-title" className="mt-1 text-2xl font-bold tracking-tight sm:text-[28px]">Your shopping groups</h1>
            <p className="mt-2 text-sm text-slate-600">Paste your group chat. Turn everyone's requests into one shared order.</p>
          </div>
          <button type="button" onClick={() => setShowCreate((visible) => !visible)} className="button-primary">
            {showCreate ? <X size={16} /> : <Plus size={16} />}
            {showCreate ? "Cancel" : "New group"}
          </button>
        </div>

        {showCreate && (
          <form onSubmit={onCreate} className="mb-5 flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:flex-row sm:items-end">
            <div className="flex-1">
              <label className="block text-xs font-semibold text-slate-600" htmlFor="group-name">Group name</label>
              <input
                id="group-name"
                value={name}
                onChange={(event) => onNameChange(event.target.value)}
                placeholder="e.g. Workshop supplies"
                maxLength={120}
                className="field mt-1.5"
                required
                autoFocus
              />
            </div>
            <button type="submit" disabled={loading || !name.trim()} className="button-primary justify-center sm:min-w-36">
              {loading ? <LoaderCircle className="animate-spin" size={16} /> : <Plus size={16} />}
              Create group
            </button>
          </form>
        )}

        {groups.length ? (
          <div className="grid gap-3 md:grid-cols-2">
            {groups.map((item) => (
              <button key={item.id} onClick={() => onOpen(item.id)} className="group-card text-left">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="truncate text-base font-semibold">{item.name}</h2>
                    <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                      <span className="inline-flex items-center gap-1.5"><ClipboardList size={14} />{item.itemCount} {item.itemCount === 1 ? "product" : "products"}</span>
                      <span aria-hidden="true" className="text-slate-300">·</span>
                      <span>{item.itemCount > 0 ? "Order created" : "Not analyzed yet"}</span>
                    </div>
                  </div>
                  <ArrowRight size={17} className="mt-0.5 shrink-0 text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-emerald-700" />
                </div>
                <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
                  <span className="text-[11px] text-slate-400">Created {new Date(item.createdAt).toLocaleDateString()}</span>
                  <span className="text-xs font-semibold text-emerald-800">Open group</span>
                </div>
              </button>
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
            <span className="mx-auto grid h-10 w-10 place-items-center rounded-xl bg-slate-100 text-slate-500"><ShoppingBasket size={20} /></span>
            <h2 className="mt-3 text-sm font-semibold">No shopping groups yet</h2>
            <p className="mt-1 text-sm text-slate-500">Create a group to start turning chat messages into an order.</p>
            <button type="button" onClick={() => setShowCreate(true)} className="button-quiet mx-auto mt-4"><Plus size={15} /> Create your first group</button>
          </div>
        )}
      </section>
    </div>
  );
}

type WorkspaceProps = {
  group: ShoppingGroup;
  conversation: string;
  loading: boolean;
  processingStep: number;
  sourceRequest: ShoppingRequest | null;
  addOpen: boolean;
  editingItem: OrderItem | null;
  onHome: () => void;
  onConversationChange: (value: string) => void;
  onDemo: () => void;
  onAnalyze: () => void;
  onRequestClick: (request: ShoppingRequest) => void;
  onCloseSource: () => void;
  onEdit: (item: OrderItem) => void;
  onSaveEdit: (item: OrderItem, name: string, quantity: number, unit: string) => Promise<boolean | undefined>;
  onCloseEdit: () => void;
  onDelete: (itemId: string) => void;
  onStatus: (item: OrderItem) => void;
  onAdd: () => void;
  onCloseAdd: () => void;
  onAddSubmit: (name: string, quantity: number, unit: string) => void;
};

function Workspace(props: WorkspaceProps) {
  const { group } = props;
  const ambiguous = useMemo(() => group.requests.filter((request) => request.ambiguous), [group.requests]);
  const completedCount = group.items.filter((item) => item.status === "COMPLETED").length;

  return (
    <div className="animate-in">
      <div className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <button onClick={props.onHome} className="mb-3 inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-emerald-800">
            <ArrowLeft size={14} /> All groups
          </button>
          <p className="eyebrow">Shopping group</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">{group.name}</h1>
          <p className="mt-1.5 text-sm text-slate-600">Paste the conversation and turn everyone's requests into one shared order.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <div className="stat-pill"><ClipboardList size={15} /><strong>{group.items.length}</strong> products</div>
          <div className="stat-pill"><CheckCircle2 size={15} /><strong>{completedCount}</strong> done</div>
        </div>
      </div>
      <div className="mt-5 grid items-start gap-4 lg:grid-cols-2">
        <ConversationPanel {...props} />
        <OrderPanel
          group={group}
          ambiguous={ambiguous}
          sourceRequest={props.sourceRequest}
          addOpen={props.addOpen}
          editingItem={props.editingItem}
          onRequestClick={props.onRequestClick}
          onCloseSource={props.onCloseSource}
          onEdit={props.onEdit}
          onSaveEdit={props.onSaveEdit}
          onCloseEdit={props.onCloseEdit}
          onDelete={props.onDelete}
          onStatus={props.onStatus}
          onAdd={props.onAdd}
          onCloseAdd={props.onCloseAdd}
          onAddSubmit={props.onAddSubmit}
        />
      </div>
      <HowItWorks />
    </div>
  );
}

function HowItWorks() {
  return (
    <section aria-label="How BuyTogether works" className="mt-4 rounded-xl border border-slate-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold">How it works</h2>
        <span className="text-[11px] text-slate-500">From chat to shared order</span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="flex gap-3 rounded-lg bg-slate-50 p-3">
          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-emerald-100 text-emerald-800"><Sparkles size={16} /></span>
          <p className="text-xs leading-5 text-slate-600"><strong className="block text-slate-900">Gemma 4 extracts requests</strong>Finds who wants what and the requested quantity.</p>
        </div>
        <div className="flex gap-3 rounded-lg bg-slate-50 p-3">
          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-slate-200 text-slate-700"><ClipboardList size={16} /></span>
          <p className="text-xs leading-5 text-slate-600"><strong className="block text-slate-900">Java builds the order</strong>Validates quantities, normalizes products, and aggregates matching requests.</p>
        </div>
      </div>
    </section>
  );
}

function ConversationPanel({ group, conversation, loading, processingStep, onConversationChange, onDemo, onAnalyze }: WorkspaceProps) {
  return (
    <section className="panel-card">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2"><span className="section-icon bg-slate-100 text-slate-700"><MessageCircle size={17} /></span><h2 className="text-base font-semibold">Group conversation</h2></div>
          <p className="mt-1.5 text-sm text-slate-500">Paste your group chat. Gemma 4 extracts each person's shopping requests.</p>
        </div>
        <span className="hidden rounded-md bg-slate-100 px-2 py-1 text-[10px] font-semibold text-slate-500 sm:inline">STEP 1</span>
      </div>
      <div className="mt-4 rounded-lg border border-slate-200 bg-white">
        <textarea
          aria-label="Paste group conversation"
          value={conversation}
          onChange={(event) => onConversationChange(event.target.value)}
          placeholder={"Paste your group chat...\n\nFor example:\nRahul: Need 3 blue pens for the workshop.\nAman: I need five blue pens too."}
          className="min-h-[220px] w-full resize-y rounded-lg bg-transparent p-3.5 text-sm leading-6 text-slate-800 outline-none placeholder:text-slate-400 focus:ring-2 focus:ring-emerald-600/15 sm:min-h-[260px]"
          maxLength={20000}
        />
        <div className="flex justify-between border-t border-slate-100 px-3 py-2 text-[11px] text-slate-400">
          <span>Paste messages as they appear</span><span>{conversation.length.toLocaleString()} / 20,000</span>
        </div>
      </div>
      <div className="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-between">
        <button onClick={onDemo} className="button-quiet justify-center"><WandSparkles size={15} /> Load demo</button>
        <button onClick={onAnalyze} disabled={loading || !conversation.trim()} className="button-primary justify-center">
          {loading ? <LoaderCircle size={16} className="animate-spin" /> : <Sparkles size={16} />}
          {loading ? "Analyzing conversation…" : "Analyze conversation"}
          {!loading && <ArrowRight size={15} />}
        </button>
      </div>
      {loading && <ProcessingState activeStep={processingStep} />}
      {group.requests.length > 0 && <p className="mt-4 text-xs text-slate-400">Analyzing new chat replaces this group’s previous extracted requests and generated products.</p>}
    </section>
  );
}

function ProcessingState({ activeStep }: { activeStep: number }) {
  const steps = ["Reading messages", "Extracting requests", "Aggregating products", "Building shared order"];
  return (
    <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-3" role="status" aria-live="polite">
      <p className="mb-2 text-xs font-semibold text-slate-700">Processing conversation</p>
      <ol className="grid gap-2 sm:grid-cols-2">
        {steps.map((step, index) => (
          <li key={step} className={`flex items-center gap-2 text-xs ${index <= activeStep ? "text-emerald-800" : "text-slate-400"}`}>
            <span className={`grid h-5 w-5 place-items-center rounded-full text-[10px] font-bold ${index < activeStep ? "bg-emerald-700 text-white" : index === activeStep ? "border border-emerald-600 bg-white text-emerald-700" : "border border-slate-300 bg-white"}`}>
              {index < activeStep ? <Check size={12} /> : index + 1}
            </span>
            {step}
          </li>
        ))}
      </ol>
    </div>
  );
}

type OrderPanelProps = {
  group: ShoppingGroup;
  ambiguous: ShoppingRequest[];
  sourceRequest: ShoppingRequest | null;
  addOpen: boolean;
  editingItem: OrderItem | null;
  onRequestClick: (request: ShoppingRequest) => void;
  onCloseSource: () => void;
  onEdit: (item: OrderItem) => void;
  onSaveEdit: (item: OrderItem, name: string, quantity: number, unit: string) => Promise<boolean | undefined>;
  onCloseEdit: () => void;
  onDelete: (id: string) => void;
  onStatus: (item: OrderItem) => void;
  onAdd: () => void;
  onCloseAdd: () => void;
  onAddSubmit: (name: string, quantity: number, unit: string) => void;
};

function OrderPanel(props: OrderPanelProps) {
  const { group, ambiguous } = props;
  const hasResults = group.requests.length > 0 || group.items.length > 0;
  return (
    <section className="panel-card min-h-[360px]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2"><span className="section-icon bg-emerald-50 text-emerald-800"><ShoppingBasket size={17} /></span><h2 className="text-base font-semibold">Shared shopping order</h2></div>
          <p className="mt-1.5 text-sm text-slate-500">Java validates and combines matching products.</p>
        </div>
        <button onClick={props.onAdd} className="button-outline shrink-0"><Plus size={15} /><span className="hidden sm:inline">Add item</span></button>
      </div>
      {hasResults && (
        <div className="mt-5 flex flex-wrap gap-2">
          <span className="result-chip bg-violet-50 text-violet-800"><Sparkles size={13} /> Gemma found {group.requests.length} requests</span>
          <span className="result-chip bg-emerald-50 text-emerald-800"><CheckCircle2 size={13} /> {group.items.length} products created</span>
        </div>
      )}
      {!hasResults ? (
        <div className="mt-5 grid min-h-[265px] place-items-center rounded-lg border border-dashed border-slate-200 bg-slate-50 px-6 text-center">
          <div>
            <span className="mx-auto grid h-11 w-11 place-items-center rounded-xl bg-white text-slate-400"><ShoppingBasket size={22} /></span>
            <h3 className="mt-3 text-sm font-semibold">Your shared order will appear here</h3>
            <p className="mx-auto mt-1.5 max-w-[270px] text-xs leading-5 text-slate-500">Load the demo or paste a group chat, then analyze the conversation.</p>
          </div>
        </div>
      ) : (
        <div className="mt-5 space-y-3">
          {group.items.map((item) => (
            <ProductCard
              key={item.id}
              item={item}
              onEdit={props.onEdit}
              onDelete={props.onDelete}
              onStatus={props.onStatus}
              onRequestClick={props.onRequestClick}
            />
          ))}
          {ambiguous.map((request) => (
            <button key={request.id} onClick={() => props.onRequestClick(request)} className="flex w-full items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/70 p-4 text-left transition hover:bg-amber-50">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-amber-100 text-amber-800"><CircleHelp size={18} /></span>
              <span className="min-w-0 flex-1">
                <span className="block font-bold text-amber-950">{request.normalizedItem} <span className="ml-1 text-xs font-semibold text-amber-800">· Quantity required</span></span>
                <span className="mt-1 block text-sm text-amber-900/70">{request.person}: {request.ambiguityReason ?? "Please clarify the quantity."}</span>
              </span>
              <ChevronRight size={16} className="mt-1 shrink-0 text-amber-700" />
            </button>
          ))}
        </div>
      )}
      {group.requests.length > 0 && (
        <details className="mt-5 rounded-2xl border border-slate-200 bg-slate-50/70">
          <summary className="cursor-pointer list-none px-4 py-3 text-sm font-bold text-slate-700 marker:hidden">
            <span className="inline-flex items-center gap-2">
              <ClipboardList size={16} className="text-sky-700" />
              Structured requests
              <span className="rounded-full bg-white px-2 py-0.5 text-xs text-slate-500">{group.requests.length}</span>
            </span>
          </summary>
          <div className="space-y-2 border-t border-slate-200 p-3">
            {group.requests.map((request) => (
              <button
                key={request.id}
                onClick={() => props.onRequestClick(request)}
                className="flex w-full items-center justify-between gap-3 rounded-xl bg-white px-3 py-2.5 text-left transition hover:bg-emerald-50"
              >
                <span className="min-w-0">
                  <span className="block truncate text-sm font-semibold text-slate-800">
                    {request.person} · {request.normalizedItem}
                  </span>
                  <span className="mt-0.5 block truncate text-xs text-slate-500">
                    {request.ambiguous || request.quantity === null
                      ? "Quantity required"
                      : `${request.quantity} ${request.unit}`}
                  </span>
                </span>
                <ChevronRight size={15} className="shrink-0 text-slate-400" />
              </button>
            ))}
          </div>
        </details>
      )}
      {props.addOpen && <AddItemModal onClose={props.onCloseAdd} onSubmit={props.onAddSubmit} />}
      {props.editingItem && (
        <EditItemModal
          item={props.editingItem}
          onClose={props.onCloseEdit}
          onSubmit={props.onSaveEdit}
        />
      )}
      {props.sourceRequest && <SourceModal request={props.sourceRequest} onClose={props.onCloseSource} />}
    </section>
  );
}

type ProductCardProps = {
  item: OrderItem;
  onEdit: (item: OrderItem) => void;
  onDelete: (id: string) => void;
  onStatus: (item: OrderItem) => void;
  onRequestClick: (request: ShoppingRequest) => void;
};

function ProductCard({ item, onEdit, onDelete, onStatus, onRequestClick }: ProductCardProps) {
  const complete = item.status === "COMPLETED";
  const sourceTotals = item.requests.reduce<Map<string, { quantity: number; request: ShoppingRequest }>>((totals, request) => {
    if (request.quantity === null) return totals;
    const key = `${request.person}\u0000${request.unit}`;
    const current = totals.get(key);
    totals.set(key, {
      quantity: (current?.quantity ?? 0) + request.quantity,
      request: current?.request ?? request,
    });
    return totals;
  }, new Map());
  return (
    <article className={`rounded-lg border p-4 transition ${complete ? "border-emerald-200 bg-emerald-50/40" : "border-slate-200 bg-white"}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <h3 className={`text-base font-extrabold ${complete ? "text-slate-500 line-through" : "text-slate-900"}`}>{item.name}</h3>
            {complete && <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-800">DONE</span>}
          </div>
          <p className="mt-1 text-sm text-slate-500">Total: <strong className="text-slate-800">{item.totalQuantity} {item.unit}</strong></p>
        </div>
        <button aria-label={complete ? `Reopen ${item.name}` : `Complete ${item.name}`} onClick={() => onStatus(item)} className={`grid h-8 w-8 shrink-0 place-items-center rounded-full border transition ${complete ? "border-emerald-300 bg-emerald-600 text-white" : "border-slate-200 text-slate-400 hover:border-emerald-400 hover:text-emerald-700"}`}>
          {complete ? <Check size={16} /> : <CheckCircle2 size={17} />}
        </button>
      </div>
      {sourceTotals.size > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {[...sourceTotals.entries()].map(([key, source]) => (
            <button key={key} onClick={() => onRequestClick(source.request)} className="person-chip hover:border-emerald-300 hover:bg-emerald-50">
              <span>{source.request.person}</span><span className="text-slate-400">—</span><strong>{source.quantity} {source.request.unit}</strong>
            </button>
          ))}
        </div>
      )}
      <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
        {item.requests.length ? <span className="text-[11px] text-slate-400">Select a request to view its source</span> : <span className="text-[11px] text-slate-400">Manually added product</span>}
        <div className="flex gap-1">
          <button onClick={() => onEdit(item)} className="action-link">Edit</button>
          <button onClick={() => onDelete(item.id)} className="action-link text-rose-600 hover:bg-rose-50" aria-label={`Delete ${item.name}`}><Trash2 size={13} /> Delete</button>
        </div>
      </div>
    </article>
  );
}

function SourceModal({ request, onClose }: { request: ShoppingRequest; onClose: () => void }) {
  return (
    <Modal onClose={onClose}>
      <div className="flex items-start justify-between">
        <div><p className="eyebrow">Extracted request</p><h2 className="mt-1 text-xl font-extrabold">{request.person}</h2></div>
        <button onClick={onClose} className="grid h-8 w-8 place-items-center rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200" aria-label="Close"><X size={17} /></button>
      </div>
      <div className="mt-5 rounded-2xl bg-emerald-50 p-4">
        <p className="font-bold text-emerald-950">{request.normalizedItem}</p>
        <p className="mt-1 text-sm text-emerald-900/70">Quantity: {request.quantity ?? "Needs clarification"} {request.quantity !== null ? request.unit : ""}</p>
      </div>
      <p className="mt-5 text-xs font-bold uppercase tracking-wide text-slate-400">Original source message</p>
      <blockquote className="mt-2 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">“{request.originalText}”</blockquote>
      <button onClick={onClose} className="button-primary mt-5 w-full justify-center">Done</button>
    </Modal>
  );
}

function AddItemModal({ onClose, onSubmit }: { onClose: () => void; onSubmit: (name: string, quantity: number, unit: string) => void }) {
  const [name, setName] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [unit, setUnit] = useState("pieces");
  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const amount = Number(quantity);
    if (!Number.isInteger(amount) || amount <= 0) return;
    onSubmit(name.trim(), amount, unit.trim());
  };
  return (
    <Modal onClose={onClose}>
      <div className="flex items-start justify-between"><div><p className="eyebrow">Shared order</p><h2 className="mt-1 text-xl font-extrabold">Add a product</h2></div><button onClick={onClose} className="grid h-8 w-8 place-items-center rounded-full bg-slate-100 text-slate-500" aria-label="Close"><X size={17} /></button></div>
      <form onSubmit={submit} className="mt-5 space-y-4">
        <label className="block text-sm font-semibold">Product<input className="field mt-1.5" value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Blue Pen" required maxLength={200} /></label>
        <div className="grid grid-cols-2 gap-3">
          <label className="block text-sm font-semibold">Quantity<input className="field mt-1.5" type="number" min="1" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
          <label className="block text-sm font-semibold">Unit<input className="field mt-1.5" value={unit} onChange={(event) => setUnit(event.target.value)} required maxLength={40} /></label>
        </div>
        <button className="button-primary w-full justify-center" type="submit"><Plus size={16} /> Add to order</button>
      </form>
    </Modal>
  );
}

function EditItemModal({
  item,
  onClose,
  onSubmit,
}: {
  item: OrderItem;
  onClose: () => void;
  onSubmit: (item: OrderItem, name: string, quantity: number, unit: string) => Promise<boolean | undefined>;
}) {
  const [name, setName] = useState(item.name);
  const [quantity, setQuantity] = useState(String(item.totalQuantity));
  const [unit, setUnit] = useState(item.unit);
  const [saving, setSaving] = useState(false);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const amount = Number(quantity);
    if (!Number.isInteger(amount) || amount <= 0 || !name.trim() || !unit.trim()) return;
    setSaving(true);
    try {
      await onSubmit(item, name.trim(), amount, unit.trim());
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal onClose={onClose}>
      <div className="flex items-start justify-between">
        <div><p className="eyebrow">Shared order</p><h2 className="mt-1 text-xl font-extrabold">Edit product</h2></div>
        <button onClick={onClose} className="grid h-8 w-8 place-items-center rounded-full bg-slate-100 text-slate-500" aria-label="Close"><X size={17} /></button>
      </div>
      <form onSubmit={submit} className="mt-5 space-y-4">
        <label className="block text-sm font-semibold">Product<input className="field mt-1.5" value={name} onChange={(event) => setName(event.target.value)} required maxLength={200} /></label>
        <div className="grid grid-cols-2 gap-3">
          <label className="block text-sm font-semibold">Quantity<input className="field mt-1.5" type="number" min="1" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
          <label className="block text-sm font-semibold">Unit<input className="field mt-1.5" value={unit} onChange={(event) => setUnit(event.target.value)} required maxLength={40} /></label>
        </div>
        <div className="flex gap-2 pt-1">
          <button type="button" onClick={onClose} disabled={saving} className="button-quiet flex-1 justify-center">Cancel</button>
          <button type="submit" disabled={saving} className="button-primary flex-1 justify-center">
            {saving ? <LoaderCircle size={16} className="animate-spin" /> : <Check size={16} />}
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  return createPortal(
    <div
      className="fixed inset-0 z-[100] grid h-[100dvh] w-screen place-items-center overflow-y-auto bg-slate-950/40 p-4 backdrop-blur-[2px]"
      onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}
    >
      <div
        role="dialog"
        aria-modal="true"
        className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl"
      >
        {children}
      </div>
    </div>,
    document.body,
  );
}

export default App;
